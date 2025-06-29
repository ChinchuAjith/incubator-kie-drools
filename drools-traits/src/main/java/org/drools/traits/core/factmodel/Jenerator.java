/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.drools.traits.core.factmodel;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import org.drools.compiler.builder.impl.classbuilder.ClassBuilder;
import org.drools.base.factmodel.ClassDefinition;
import org.drools.base.factmodel.FieldDefinition;
import org.drools.util.IoUtils;

import static org.kie.utll.xml.XStreamUtils.createNonTrustingXStream;

/**
 * This will generate a jar from a meta model.
 */
public class Jenerator {

    private final ClassLoader classLoader;

    public Jenerator() {
        this(Jenerator.class.getClassLoader());
    }

    public Jenerator(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public byte[] createJar(Fact[] facts, String packageName) throws SecurityException, IllegalArgumentException, IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        JarOutputStream jout = new JarOutputStream(result);

        JarEntry metaModel = new JarEntry("factmodel.xml");
        jout.putNextEntry(metaModel);
        jout.write(toXML(facts));
        jout.closeEntry();

        String packagePath = packageName.replace('.', '/');

        for (int i = 0; i < facts.length; i++) {
            ClassBuilder cb = new TraitClassBuilderFactory().getBeanClassBuilder();
            ClassDefinition classDef = new ClassDefinition(packageName, null, new String[]{"java.io.Serializable"} );
            for (int j = 0; j < facts[i].fields.size(); j++) {
                Field fd = (Field) facts[i].fields.get(j);
                classDef.addField(new FieldDefinition(fd.name, fd.type));
            }
            JarEntry je = new JarEntry(packagePath + "/" + facts[i].name + ".class");
            jout.putNextEntry(je);
            jout.write(cb.buildClass(classDef, classLoader));
            jout.closeEntry();
        }
        jout.flush();
        jout.close();

        return result.toByteArray();
    }

    private byte[] toXML(Fact[] facts) {
        XStream x = createNonTrustingXStream(new DomDriver());
        return x.toXML(facts).getBytes(IoUtils.UTF8_CHARSET);

    }

    public Fact[] loadMetaModel(JarInputStream jis) throws Exception {
        JarEntry entry;
        while ( (entry = jis.getNextJarEntry()) != null ) {
            if (entry.getName().equals("factmodel.xml")) {
                return fromXML(jis);
            }
        }

        throw new IllegalArgumentException("This is not a valid drools model jar - no factmodel.xml found.");
    }

    private Fact[] fromXML(JarInputStream jis) {
        XStream x = createNonTrustingXStream(new DomDriver());
        return (Fact[]) x.fromXML(jis);

    }


}
