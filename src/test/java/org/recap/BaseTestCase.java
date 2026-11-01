package org.recap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.recap.repository.jpa.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSessionEvent;

@ExtendWith({SpringExtension.class})
@SpringBootTest(classes = Main.class)
@Transactional
@Rollback()
@TestPropertySource("classpath:application.properties")
public class BaseTestCase {

    @Autowired
    public BibliographicDetailsRepository bibliographicDetailsRepository;

    @Autowired
    public ItemDetailsRepository itemDetailsRepository;

    @Autowired
    public HoldingsDetailsRepository holdingDetailRepository;

    @Autowired
    public ItemStatusDetailsRepository itemStatusDetailsRepository;

    @Autowired
    public InstitutionDetailsRepository institutionDetailRepository;

    @Autowired
    public CollectionGroupDetailsRepository collectionGroupDetailRepository;

    @Autowired
    public ItemChangeLogDetailsRepository itemChangeLogDetailsRepository;

    @Autowired
    public OwnerCodeDetailsRepository ownerCodeDetailsRepository;

    @Autowired
    public RequestItemDetailsRepository requestItemDetailsRepository;

    @Autowired
    public UserDetailsRepository userRepo;

    @Autowired
    public RolesDetailsRepositorty roleRepository;

    @Autowired
    public PermissionsDetailsRepository permissionsRepository;

    @Autowired
    public RequestTypeDetailsRepository requestTypeDetailsRepository;

   /* protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext applicationContext;

    protected SecurityManager securityManager;*/


    @Test
    public void loadContexts() {
//        System.out.println();
    }

    protected void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
    }
/*
    @BeforeEach
    public void loadApplicationContexts() {
        this.mockMvc = webAppContextSetup(applicationContext).build();
        assertNotNull(applicationContext);
        securityManager = (SecurityManager) applicationContext.getBean("securityManager");
        assertNotNull(securityManager);
    }*/
}