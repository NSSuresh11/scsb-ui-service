package org.recap.repository.jpa;

import org.recap.model.jpa.ItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Created by chenchulakshmig on 21/6/16.
 */
@RepositoryRestResource(collectionResourceRel = "item", path = "item")
public interface ItemDetailsRepository extends BaseRepository<ItemEntity> {

    /**
     * To get the count of non deleted items in scsb for the given institution.
     *
     * @param institutionId the institution id
     * @return the long
     */
    Long countByOwningInstitutionIdAndIsDeletedFalse(Integer institutionId);

    /**
     * To get the pageable of non deleted items in scsb for the given institution.
     *
     * @param pageable      the pageable
     * @param institutionId the institution id
     * @return the page
     */
    Page<ItemEntity> findByOwningInstitutionIdAndIsDeletedFalse(Pageable pageable, Integer institutionId);

    /**
     * To get the list of item entities for the given institution.
     *
     * @param owningInstitutionId the owning institution id
     * @return the list
     */
    List<ItemEntity> findByOwningInstitutionId(Integer owningInstitutionId);

    /**
     * To get the item entity for the given owning institution item id.
     *
     * @param owningInstitutionItemId the owning institution item id
     * @return the item entity
     */
    ItemEntity findByOwningInstitutionItemId(@Param("owningInstitutionItemId") String owningInstitutionItemId);

    /**
     * To get the list of item entities for the given barcode.
     *
     * @param barcode the barcode
     * @return the list
     */
    List<ItemEntity> findByBarcode(String barcode);

    /**
     * To get the list of non deleted items in scsb for the given barcode and cataloging status.
     *
     * @param barcode          the barcode
     * @param catalogingStatus the cataloging status
     * @return the list
     */
    List<ItemEntity> findByBarcodeAndCatalogingStatusAndIsDeletedFalse(String barcode, String catalogingStatus);

    /**
     * To update the item details in scsb such as collection group , last updated by and last updated date in scsb for the given item id.
     *
     * @param collectionGroupId the collection group id
     * @param itemId            the item id
     * @param lastUpdatedBy     the last updated by
     * @param lastUpdatedDate   the last updated date
     * @return the int
     */
    @Modifying
    @Transactional
    @Query("update ItemEntity item set item.collectionGroupId = :collectionGroupId, item.lastUpdatedBy = :lastUpdatedBy, item.lastUpdatedDate = :lastUpdatedDate where item.id = :itemId")
    int updateCollectionGroupIdById(@Param("collectionGroupId") Integer collectionGroupId, @Param("itemId") Integer itemId, @Param("lastUpdatedBy") String lastUpdatedBy, @Param("lastUpdatedDate") Date lastUpdatedDate);

    /**
     * To get the status of the non deleted item in scsb for the given barcode.
     *
     * @param barcode the barcode
     * @return the item status by barcode and is deleted false
     */
    @Query(value = "select itemStatus.statusCode from ItemEntity item, ItemStatusEntity itemStatus where item.itemAvailabilityStatusId = itemStatus.id and item.barcode = :barcode and item.isDeleted = :deleted")
    String getItemStatusByBarcodeAndIsDeletedFalse(@Param("barcode") String barcode,@Param("deleted") Boolean deleted);


    /**
     *
     * @param itemBarcode
     * @return Instituiton COde
     */

    @Query(value = "SELECT INSTITUTION_CODE" +
            "    FROM ITEM_T" +
            "    INNER JOIN INSTITUTION_T ON ITEM_T.OWNING_INST_ID = INSTITUTION_T.INSTITUTION_ID" +
            "    WHERE ITEM_T.BARCODE = :itemBarcode",nativeQuery = true)
    String findInstitutionCodeByBarcode(@Param("itemBarcode") String itemBarcode);
}
