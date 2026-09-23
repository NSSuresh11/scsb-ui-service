package org.recap.security.saml;

import lombok.extern.slf4j.Slf4j;
import org.recap.model.saml.SamlConfig;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.zip.Deflater;

@Slf4j
@Component
public class SamlAuthHandler {

    private static final String SAML_NS  = "urn:oasis:names:tc:SAML:2.0:assertion";
    private static final String SAMLP_NS = "urn:oasis:names:tc:SAML:2.0:protocol";
    private static final String DSIG_NS  = "http://www.w3.org/2000/09/xmldsig#";

    public static class SamlUserInfo {
        public final String userId;
        public final String displayName;
        public final String email;
        public final String uid;
        public final String institutionCode;

        public SamlUserInfo(String userId, String displayName, String email,
                            String uid, String institutionCode) {
            this.userId          = userId;
            this.displayName     = (displayName != null && !displayName.isEmpty()) ? displayName : userId;
            this.email           = email;
            this.uid             = uid;
            this.institutionCode = institutionCode;
        }

        @Override
        public String toString() {
            return "SamlUserInfo{userId='" + userId + "', institutionCode='" + institutionCode + "'}";
        }
    }

    //  Build AuthnRequest (HTTP-Redirect binding)

    public String buildAuthnRequestUrl(SamlConfig config, String callbackUrl) {
        return buildAuthnRequestUrl(config, callbackUrl, null);
    }

    public String buildAuthnRequestUrl(SamlConfig config, String callbackUrl, String relayState) {
        try {
            String requestId    = "_" + java.util.UUID.randomUUID();
            String issueInstant = java.time.Instant.now().toString();
            String spEntityId   = resolveSpEntityId(config, callbackUrl);
            String effectiveAcs = (config.getAcsUrl() != null && !config.getAcsUrl().isBlank())
                    ? config.getAcsUrl() : callbackUrl;
            String ssoUrl = config.getIdpSsoUrl();

            String authnRequest =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                            + "<samlp:AuthnRequest"
                            + " xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\""
                            + " xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\""
                            + " ID=\"" + requestId + "\""
                            + " Version=\"2.0\""
                            + " IssueInstant=\"" + issueInstant + "\""
                            + " Destination=\"" + ssoUrl + "\""
                            + " AssertionConsumerServiceURL=\"" + effectiveAcs + "\""
                            + " ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\">"
                            + "<saml:Issuer>" + spEntityId + "</saml:Issuer>"
                            + "<samlp:NameIDPolicy"
                            + " Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\""
                            + " AllowCreate=\"true\"/>"
                            + "</samlp:AuthnRequest>";

            log.info("SAML [{}]: AuthnRequest ID={}", config.getInstitutionCode(), requestId);

            byte[]   authnBytes = authnRequest.getBytes(StandardCharsets.UTF_8);
            Deflater deflater   = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
            deflater.setInput(authnBytes);
            deflater.finish();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buf);
                baos.write(buf, 0, count);
            }
            deflater.end();

            String encoded = Base64.getEncoder().encodeToString(baos.toByteArray());
            String separator = ssoUrl.contains("?") ? "&" : "?";
            StringBuilder redirectUrl = new StringBuilder(ssoUrl)
                    .append(separator)
                    .append("SAMLRequest=")
                    .append(URLEncoder.encode(encoded, StandardCharsets.UTF_8));

            //  carry institutionCode in RelayState so ACS works even
            //         when the POST lands on a different node (no sticky sessions)
            if (relayState != null && !relayState.isBlank()) {
                redirectUrl.append("&RelayState=")
                        .append(URLEncoder.encode(relayState, StandardCharsets.UTF_8));
            }

            log.info("SAML [{}]: Redirecting to IdP SSO: {}", config.getInstitutionCode(), ssoUrl);
            return redirectUrl.toString();

        } catch (Exception e) {
            log.error("SAML [{}]: Failed to build AuthnRequest", config.getInstitutionCode(), e);
            throw new RuntimeException("Failed to build SAML AuthnRequest", e);
        }
    }

    // Process and validate the IdP's SAMLResponse POST
    public SamlUserInfo processSamlResponse(String samlResponseBase64, SamlConfig config) {
        try {
            byte[] responseBytes = Base64.getDecoder().decode(samlResponseBase64);
            String responseXml   = new String(responseBytes, StandardCharsets.UTF_8);

            log.debug("SAML [{}]: Response XML length={}", config.getInstitutionCode(), responseXml.length());

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            // XXE hardening
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(responseXml)));

            // Verify SAML StatusCode == Success
            NodeList statusCodes = doc.getElementsByTagNameNS(SAMLP_NS, "StatusCode");
            if (statusCodes.getLength() > 0) {
                String statusValue = ((Element) statusCodes.item(0)).getAttribute("Value");
                if (!"urn:oasis:names:tc:SAML:2.0:status:Success".equals(statusValue)) {
                    log.error("SAML [{}]: Status not Success: {}", config.getInstitutionCode(), statusValue);
                    return null;
                }
            }

            //  Register all ID attributes so enveloped-signature refs resolve
            registerIdAttributes(doc);

            // Cryptographic signature verification
            X509Certificate idpCert = loadCertificateFromPem(config.getIdpCertificate());
            if (!validateSignature(doc, idpCert, config.getInstitutionCode())) {
                log.error("SAML [{}]: XML signature validation FAILED", config.getInstitutionCode());
                return null;
            }
            log.info("SAML [{}]: XML signature validated OK", config.getInstitutionCode());

            //  Extract NameID
            NodeList nameIdNodes = doc.getElementsByTagNameNS(SAML_NS, "NameID");
            if (nameIdNodes.getLength() == 0) {
                log.error("SAML [{}]: No NameID in assertion", config.getInstitutionCode());
                return null;
            }
            String userId = nameIdNodes.item(0).getTextContent().trim();
            if (userId.isEmpty()) {
                log.error("SAML [{}]: NameID is empty", config.getInstitutionCode());
                return null;
            }

            //  Extract optional attributes
            String displayName = extractAttribute(doc, "displayName");
            String email       = extractAttribute(doc, "email");
            String uid         = extractAttribute(doc, "uid");

            // Okta often sends email as NameID ? normalize to bare username
            if (userId.contains("@")) {
                log.info("SAML [{}]: NameID is email '{}', using full email as userId", config.getInstitutionCode(), userId);
                if (email == null) email = userId;          // preserve full email for record
//                userId = userId.split("@")[0];              // strip domain ? matches USER_T.LOGIN_ID
            }

            SamlUserInfo userInfo = new SamlUserInfo(userId, displayName, email, uid, config.getInstitutionCode());
            log.info("SAML [{}]: Authenticated user: {}", config.getInstitutionCode(), userInfo);
            return userInfo;

        } catch (Exception e) {
            log.error("SAML [{}]: Failed to process SAML response", config.getInstitutionCode(), e);
            return null;
        }
    }

    //  Generate SP metadata XML
    public String generateSpMetadata(SamlConfig config) {
        String spEntityId = config.getSpEntityId();
        String acsUrl     = config.getAcsUrl();
        String spCertBody = extractCertBody(config.getSpCertificate());

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\"\n");
        sb.append("                     xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"\n");
        sb.append("                     entityID=\"").append(spEntityId).append("\"\n");
        sb.append("                     validUntil=\"2036-01-01T00:00:00Z\"\n");
        sb.append("                     cacheDuration=\"PT48H\">\n");
        sb.append("    <md:SPSSODescriptor AuthnRequestsSigned=\"false\"\n");  // we use redirect, not signed req
        sb.append("                        WantAssertionsSigned=\"true\"\n");
        sb.append("                        protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n");
        if (spCertBody != null && !spCertBody.isEmpty()) {
            appendKeyDescriptor(sb, "signing",    spCertBody);
            appendKeyDescriptor(sb, "encryption", spCertBody);
        }
        sb.append("        <md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat>\n");
        sb.append("        <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</md:NameIDFormat>\n");
        sb.append("        <md:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:emailAddress</md:NameIDFormat>\n");
        sb.append("        <md:AssertionConsumerService\n");
        sb.append("             Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\"\n");
        sb.append("             Location=\"").append(acsUrl).append("\"\n");
        sb.append("             index=\"0\" isDefault=\"true\"/>\n");
        sb.append("    </md:SPSSODescriptor>\n");
        sb.append("</md:EntityDescriptor>");
        return sb.toString();
    }

    // ??? FIXED: Real cryptographic signature validation ???????????????????????

    private boolean validateSignature(Document doc, X509Certificate trustedIdpCert,
                                      String institutionCode) {
        try {
            NodeList signatures = doc.getElementsByTagNameNS(DSIG_NS, "Signature");
            if (signatures.getLength() == 0) {
                log.warn("SAML [{}]: No XML Signature element found ? rejecting", institutionCode);
                return false;
            }

            // Use the trusted IdP public key directly ? do NOT rely on embedded cert alone.
            // javax.xml.crypto.dsig verifies both the digest over the signed elements
            // AND the RSA/DSA signature over the SignedInfo, using trustedIdpCert.getPublicKey().
            DOMValidateContext valCtx = new DOMValidateContext(
                    trustedIdpCert.getPublicKey(),
                    signatures.item(0)
            );

            XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");
            XMLSignature sig = fac.unmarshalXMLSignature(valCtx);
            boolean valid = sig.validate(valCtx);

            if (!valid) {
                // Emit detailed diagnostic
                boolean sv = sig.getSignatureValue().validate(valCtx);
                log.error("SAML [{}]: Signature invalid ? signatureValue={}", institutionCode, sv);
                for (Object ref : sig.getSignedInfo().getReferences()) {
                    javax.xml.crypto.dsig.Reference r = (javax.xml.crypto.dsig.Reference) ref;
                    log.error("SAML [{}]:   Reference URI='{}' valid={}", institutionCode, r.getURI(), r.validate(valCtx));
                }
            } else {
                log.info("SAML [{}]: Signature cryptographically verified OK", institutionCode);
            }
            return valid;

        } catch (Exception e) {
            log.error("SAML [{}]: Signature validation threw exception", institutionCode, e);
            return false;
        }
    }

    /**
     * Register ID attributes on all elements that carry them.
     * Required so that enveloped-signature URI references (URI="#id123") resolve correctly.
     */
    private void registerIdAttributes(Document doc) {
        String[] idAttrNames = {"ID", "Id", "id"};
        registerIdOnElements(doc.getElementsByTagNameNS(SAMLP_NS, "Response"),    idAttrNames);
        registerIdOnElements(doc.getElementsByTagNameNS(SAML_NS,  "Assertion"),   idAttrNames);
        registerIdOnElements(doc.getElementsByTagNameNS(SAML_NS,  "EncryptedAssertion"), idAttrNames);
    }

    private void registerIdOnElements(NodeList nodes, String[] idAttrNames) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Element el = (Element) nodes.item(i);
            for (String attr : idAttrNames) {
                if (el.hasAttribute(attr)) {
                    el.setIdAttribute(attr, true);
                }
            }
        }
    }

    // ??? Certificate loading ??????????????????????????????????????????????????

    private X509Certificate loadCertificateFromPem(String pem) throws Exception {
        if (pem == null || pem.isBlank()) {
            throw new IllegalArgumentException("IdP certificate is null or blank ? check idp.certificate in scsb_properties_t");
        }

        // Handle escaped newlines stored in DB (common when inserted via SQL string literals)
        String normalised = pem.replace("\\n", "\n").trim();

        CertificateFactory cf = CertificateFactory.getInstance("X.509");

        if (normalised.contains("-----BEGIN CERTIFICATE-----")) {
            try (InputStream is = new ByteArrayInputStream(normalised.getBytes(StandardCharsets.UTF_8))) {
                return (X509Certificate) cf.generateCertificate(is);
            }
        } else {
            // Raw base64 (no PEM headers) ? stored directly in DB
            byte[] certBytes = Base64.getDecoder().decode(normalised.replaceAll("\\s+", ""));
            try (InputStream is = new ByteArrayInputStream(certBytes)) {
                return (X509Certificate) cf.generateCertificate(is);
            }
        }
    }

    private String extractCertBody(String pemCert) {
        if (pemCert == null || pemCert.isBlank()) return null;
        return pemCert
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
    }

    private String extractAttribute(Document doc, String attributeName) {
        NodeList attributes = doc.getElementsByTagNameNS(SAML_NS, "Attribute");
        for (int i = 0; i < attributes.getLength(); i++) {
            Element attr = (Element) attributes.item(i);
            if (attributeName.equals(attr.getAttribute("Name"))
                    || attributeName.equals(attr.getAttribute("FriendlyName"))) {
                NodeList values = attr.getElementsByTagNameNS(SAML_NS, "AttributeValue");
                if (values.getLength() > 0) {
                    return values.item(0).getTextContent().trim();
                }
            }
        }
        return null;
    }

    private String resolveSpEntityId(SamlConfig config, String callbackUrl) {
        if (config.getSpEntityId() != null && !config.getSpEntityId().isBlank()) {
            return config.getSpEntityId();
        }
        try {
            java.net.URL url = new java.net.URL(callbackUrl);
            return url.getProtocol() + "://" + url.getHost()
                    + (url.getPort() > 0 ? ":" + url.getPort() : "")
                    + "/saml/metadata";
        } catch (Exception e) {
            return callbackUrl;
        }
    }

    private void appendKeyDescriptor(StringBuilder sb, String use, String certBody) {
        sb.append("        <md:KeyDescriptor use=\"").append(use).append("\">\n");
        sb.append("            <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n");
        sb.append("                <ds:X509Data>\n");
        sb.append("                    <ds:X509Certificate>").append(certBody).append("</ds:X509Certificate>\n");
        sb.append("                </ds:X509Data>\n");
        sb.append("            </ds:KeyInfo>\n");
        sb.append("        </md:KeyDescriptor>\n");
    }
}