package org.recap.model.jaxb;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.recap.BaseTestCaseUT;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.util.HashMap;
import java.util.Map;


public class JAXBHandlerUT extends BaseTestCaseUT {

    JAXBHandler jaxBHandler;
    @Mock
    Object object;

    @Test
    public void testJAXBHandler() {
        jaxBHandler = JAXBHandler.getInstance();
        String res = jaxBHandler.marshal("test");
        try {
            Object objectValue = jaxBHandler.unmarshal("Test Data", object.getClass());
            Assertions.assertNotNull(objectValue);
        } catch (JAXBException e) {
        }
        Map<String, Unmarshaller> data = new HashMap<>();
        jaxBHandler.setUnmarshallerMap(data);
        Map<String, Marshaller> marshallerMap = new HashMap<>();
        Map<String, Unmarshaller> map = jaxBHandler.getUnmarshallerMap();
        jaxBHandler.setMarshallerMap(marshallerMap);
        Map<String, Marshaller> mapnew = jaxBHandler.getMarshallerMap();
    }
}
