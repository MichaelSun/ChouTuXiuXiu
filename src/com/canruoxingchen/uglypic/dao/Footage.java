package com.canruoxingchen.uglypic.dao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT. Enable "keep" sections if you want to edit. 
/**
 * Entity mapped to table FOOTAGE.
 */
public class Footage {

    private String objectId;
    private String footageIcon;
    private String footageIconName;
    private Integer footageOrderNum;
    private String footageParentId;

    public Footage() {
    }

    public Footage(String objectId) {
        this.objectId = objectId;
    }

    public Footage(String objectId, String footageIcon, String footageIconName, Integer footageOrderNum, String footageParentId) {
        this.objectId = objectId;
        this.footageIcon = footageIcon;
        this.footageIconName = footageIconName;
        this.footageOrderNum = footageOrderNum;
        this.footageParentId = footageParentId;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getFootageIcon() {
        return footageIcon;
    }

    public void setFootageIcon(String footageIcon) {
        this.footageIcon = footageIcon;
    }

    public String getFootageIconName() {
        return footageIconName;
    }

    public void setFootageIconName(String footageIconName) {
        this.footageIconName = footageIconName;
    }

    public Integer getFootageOrderNum() {
        return footageOrderNum;
    }

    public void setFootageOrderNum(Integer footageOrderNum) {
        this.footageOrderNum = footageOrderNum;
    }

    public String getFootageParentId() {
        return footageParentId;
    }

    public void setFootageParentId(String footageParentId) {
        this.footageParentId = footageParentId;
    }

}
