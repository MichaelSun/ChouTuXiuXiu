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
		Schema schema = new Schema(1, "com.canruoxingchen.uglypic.dao");

		addFootageType(schema);
		addFootage(schema);
		addNetScene(schema);

		new DaoGenerator().generateAll(schema, "../../src/");
	}
	
	private static void addTestInfo(Schema schema) {
		Entity history = schema.addEntity("Test");
		history.addIntProperty("type").primaryKey();
	}
	
	//version 1：添加素材类别
	private static void addFootageType(Schema schema) {
		Entity entity = schema.addEntity("FootAgeType");
		entity.addStringProperty("objectId").primaryKey();
		entity.addStringProperty("typeName");
		entity.addStringProperty("oldName");
		entity.addIntProperty("isDefault");
		entity.addIntProperty("orderNum");
		entity.addIntProperty("typeTarget");
	}
	
	//version 1: 添加素材
	private static void addFootage(Schema schema) {
		Entity entity = schema.addEntity("Footage");
		entity.addStringProperty("objectId").primaryKey();
		entity.addStringProperty("footageIcon");
		entity.addStringProperty("footageIconName");
		entity.addIntProperty("footageOrderNum");
		entity.addStringProperty("footageParentId");
	}
	
	private static void addNetScene(Schema schema) {
		Entity entity = schema.addEntity("NetSence");
		entity.addStringProperty("objectId").primaryKey();
		entity.addStringProperty("senceNetIcon");
		entity.addStringProperty("senceParentId");
		entity.addIntProperty("senceOrderNum");
		entity.addStringProperty("senceName");
		entity.addStringProperty("senceDescribe");
		entity.addStringProperty("inputContent");
		entity.addStringProperty("inputRect");
		entity.addStringProperty("inputFontName");
		entity.addIntProperty("inputFontSize");
		entity.addIntProperty("inputFontColor");
		entity.addIntProperty("inputFontAlignment");
		entity.addStringProperty("timeRect");
		entity.addStringProperty("timeFontName");
		entity.addIntProperty("timeFontSize");
		entity.addIntProperty("timeFontColor");
		entity.addIntProperty("timeFontAlignment");
	}
}
