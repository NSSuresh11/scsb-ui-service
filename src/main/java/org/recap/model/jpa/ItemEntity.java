package org.recap.model.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;


import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.List;

/**
 * Created by pvsubrah on 6/11/16.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "item_t", catalog = "")
@AttributeOverride(name = "id", column = @Column(name = "ITEM_ID"))
public class ItemEntity extends ItemAbstractEntity {


    @ManyToMany(mappedBy = "itemEntities")
    private List<HoldingsEntity> holdingsEntities;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "ITEM_AVAIL_STATUS_ID", insertable = false, updatable = false)
    private ItemStatusEntity itemStatusEntity;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "COLLECTION_GROUP_ID", insertable = false, updatable = false)
    private CollectionGroupEntity collectionGroupEntity;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "OWNING_INST_ID", insertable = false, updatable = false)
    private InstitutionEntity institutionEntity;

    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "IMS_LOCATION_ID", insertable = false, updatable = false)
    private ImsLocationEntity imsLocationEntity;

    @ManyToMany(mappedBy = "itemEntities")
    private List<BibliographicEntity> bibliographicEntities;

    /**
     * Instantiates a new Item entity object.
     */
    public ItemEntity() {
        super();
    }
}


