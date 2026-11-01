package org.recap.model.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.Date;

@Entity
@Table(name = "on_boarding_new_inst_t", catalog = "")
@Data
@EqualsAndHashCode(callSuper = false)
@AttributeOverride(
        name = "id",
        column = @Column(
                name = "ON_BOARD_ID"
        )
)
public class FileUploadEntity extends AbstractEntity<Integer>{
    @Column(name = "NEW_INST_NAME")
    private String newInstitutionname;
    @Column(name = "ON_BOARD_XML_FILE")
    private byte[] onBoardxmlfile;
    @Column(name = "INST_TYPE")
    private String instType;
    @Column(name = "ON_BOARD_STATUS")
    private String onBoardstatus;
    @Column(name = "ON_BOARD_COMMENTS")
    private String onBoardcomments;
    @Column(name = "CREATED_BY")
    private String createdBy;
    @Column(name = "UPDATED_BY")
    private String updatedBy;
    @Column(name = "UPDATED_DATE")
    private Date updatedDate;
    @Column(name = "CREATED_DATE")
    private Date createdDate;
}