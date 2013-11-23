/*
 * Copyright (C) 2011 Markus Junginger, greenrobot (http://greenrobot.de)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.greenrobot.daogenerator.gentest;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

/**
 * Generates entities and DAOs for the example project DaoExample.
 * 
 * Run it as a Java application (not Android).
 * 
 * @author Markus
 */
public class ExampleDaoGenerator {

	public static void main(String[] args) throws Exception {
		
		//TODO: 修改数据库时，需要升级此处的版本号
		Schema schema = new Schema(4, "com.dabanniu.hair.dao");

		// addNote(schema);
		// addCustomerOrder(schema);

		addHistoryInfo(schema);
		addPicLikeTable(schema);
		addHairStyleTable(schema);
		addWorkListTable(schema);
		addWorkLikeTable(schema);
		addRegionTable(schema);
		addColorTable(schema);
		addHairStylePackageTable(schema);
		addShowLikeTable(schema);
		addShowDisLikeTable(schema);
		addFaverateBlog(schema);

		new DaoGenerator().generateAll(schema, "../../src/");
	}
	
	private static void addTestInfo(Schema schema) {
		Entity history = schema.addEntity("Test");
		history.addIntProperty("type").primaryKey();
	}
	
	private static void addShowLikeTable(Schema schema) {
		Entity item = schema.addEntity("ShowLikeItem");
		item.addLongProperty("postId").primaryKey();
	} 
	
	private static void addShowDisLikeTable(Schema schema) {
		Entity item = schema.addEntity("ShowDisLikeItem");
		item.addLongProperty("postId").primaryKey();
	}  

	/*分包管理发型后增加*/
	private static void addHairStylePackageTable(Schema schema) {
		Entity hairStylePackage = schema.addEntity("HairStylePackageItem");
		hairStylePackage.addLongProperty("packageId").primaryKey();
		hairStylePackage.addStringProperty("zipUrl");
		hairStylePackage.addBooleanProperty("cached");
		hairStylePackage.addStringProperty("name");
		hairStylePackage.addFloatProperty("packageSize");
		hairStylePackage.addStringProperty("picUrl");
	}
	
	private static void addColorTable(Schema schema) {
		Entity color = schema.addEntity("ColorItem");
		color.addLongProperty("colorId").primaryKey();
		color.addStringProperty("name");
		color.addStringProperty("thumbnail");
		color.addStringProperty("color");
		color.addIntProperty("mapId");
		
	}  

	private static void addRegionTable(Schema schema) {
		Entity region = schema.addEntity("RegionItem");
		region.addLongProperty("districtId").primaryKey();
		region.addStringProperty("districtName");
		region.addStringProperty("cityName");
		region.addIntProperty("cityId");
		region.addStringProperty("provinceName");
		region.addIntProperty("provinceId");
		
	}  
	
	private static void addHistoryInfo(Schema schema) {
		Entity history = schema.addEntity("HistoryItem");
		history.addStringProperty("srcUri");
		history.addStringProperty("uri").primaryKey();
		history.addLongProperty("time");
		history.addIntProperty("type");
		history.addStringProperty("screenRef");
	}  


	/*2.0版本添加*/
	private static void addFaverateBlog(Schema schema) {
		Entity favBlog = schema.addEntity("FaverateBlog");
		favBlog.addLongProperty("blogId").primaryKey();
		favBlog.addIntProperty("visitorsNum");
		favBlog.addIntProperty("commentsNum");
	}
	
	private static void addPicLikeTable(Schema schema) {
		Entity picLike = schema.addEntity("PicLoveStatus");
		picLike.addLongProperty("picId").primaryKey();
		picLike.addIntProperty("loveNum");
	}

	private static void addWorkLikeTable(Schema schema) {
		Entity workLike = schema.addEntity("WorkLoveStatus");
		workLike.addLongProperty("workId").primaryKey();
		workLike.addStringProperty("thumbURL");
	}

	private static void addHairStyleTable(Schema schema) {
		Entity hairStyle = schema.addEntity("HairStyleItem");
		hairStyle.addLongProperty("styleId").primaryKey();
		hairStyle.addStringProperty("isRecent");
		hairStyle.addStringProperty("length");
		hairStyle.addStringProperty("tag");
		hairStyle.addStringProperty("thumbURL");
		hairStyle.addStringProperty("style");
		hairStyle.addStringProperty("color");
//		hairStyle.addStringProperty("localPath");
		hairStyle.addStringProperty("maskPath");
		hairStyle.addStringProperty("origPath");
		hairStyle.addStringProperty("hairContour");
		hairStyle.addStringProperty("hairPoint");
		hairStyle.addIntProperty("sortOrder");
		hairStyle.addIntProperty("colorId");
		hairStyle.addIntProperty("mapId");
		/*分包管理发型后增加*/
		hairStyle.addLongProperty("packageId");
	}

	private static void addWorkListTable(Schema schema) {
		Entity workItem = schema.addEntity("WorkItem");
		workItem.addLongProperty("workId").primaryKey();
		workItem.addStringProperty("isRecent");
		workItem.addStringProperty("length");
		workItem.addStringProperty("tag");
		workItem.addStringProperty("thumbURL");
		workItem.addStringProperty("style");
		workItem.addStringProperty("color");
	}
	

	private static void addNote(Schema schema) {
		Entity note = schema.addEntity("Note");
		note.addIdProperty();
		note.addStringProperty("text").notNull();
		note.addStringProperty("comment");
		note.addDateProperty("date");
	}

	private static void addCustomerOrder(Schema schema) {
		Entity customer = schema.addEntity("Customer");
		customer.addIdProperty();
		customer.addStringProperty("name").notNull();

		Entity order = schema.addEntity("Order");
		order.setTableName("ORDERS"); // "ORDER" is a reserved keyword
		order.addIdProperty();
		Property orderDate = order.addDateProperty("date").getProperty();
		Property customerId = order.addLongProperty("customerId").notNull()
				.getProperty();
		order.addToOne(customer, customerId);

		ToMany customerToOrders = customer.addToMany(order, customerId);
		customerToOrders.setName("orders");
		customerToOrders.orderAsc(orderDate);
	}

}
