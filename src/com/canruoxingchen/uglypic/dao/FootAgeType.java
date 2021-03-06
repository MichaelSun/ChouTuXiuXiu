package com.canruoxingchen.uglypic.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table FOOT_AGE_TYPE.
 */
public class FootAgeType {

    private String objectId;
    private String typeName;
    private String oldName;
    private Integer isDefault;
    private Integer orderNum;
    private Integer typeTarget;

    public FootAgeType() {
    }

    public FootAgeType(String objectId) {
        this.objectId = objectId;
    }

    public FootAgeType(String objectId, String typeName, String oldName, Integer isDefault, Integer orderNum, Integer typeTarget) {
        this.objectId = objectId;
        this.typeName = typeName;
        this.oldName = oldName;
        this.isDefault = isDefault;
        this.orderNum = orderNum;
        this.typeTarget = typeTarget;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public Integer getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getOrderNum() {
        return orderNum;
    }

    public void setOrderNum(Integer orderNum) {
        this.orderNum = orderNum;
    }

    public Integer getTypeTarget() {
        return typeTarget;
    }

    public void setTypeTarget(Integer typeTarget) {
        this.typeTarget = typeTarget;
    }

}
