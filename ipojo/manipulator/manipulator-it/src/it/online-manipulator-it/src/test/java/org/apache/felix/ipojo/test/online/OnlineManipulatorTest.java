/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.ipojo.test.online;


import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.test.online.components.Consumer;
import org.apache.felix.ipojo.test.online.components.MyProvider;
import org.apache.felix.ipojo.test.online.services.Hello;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.io.StreamUtils;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.ops4j.pax.tinybundles.core.TinyBundles;
import org.osgi.framework.*;
import org.ow2.chameleon.testing.helpers.OSGiHelper;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.MavenUtils.asInProject;


@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class OnlineManipulatorTest {


    private static File TMP = new File("target/tmp-bundle");

    @Inject
    BundleContext context;

    private OSGiHelper helper;


    @Configuration
    public Option[] configure() throws IOException {
        String providerWithMetadata = providerWithMetadata();
        String providerWithMetadataInMetaInf = providerWithMetadataInMetaInf();
        String providerWithoutMetadata = providerWithoutMetadata();
        String consumerWithMetadata = consumerWithMetadata();
        String consumerWithoutMetadata = consumerWithoutMetadata();

        return options(
                cleanCaches(),
                mavenBundle("org.apache.felix", "org.apache.felix.ipojo").versionAsInProject(),
                mavenBundle("org.ow2.chameleon.testing", "osgi-helpers").versionAsInProject(),
                mavenBundle("org.apache.felix","org.apache.felix.ipojo.manipulator.online").versionAsInProject(),
                junitBundles(),

                provision(
                        TinyBundles.bundle()
                                .add(Hello.class)
                                .set(Constants.BUNDLE_SYMBOLICNAME, "ServiceInterface")
                                .set(Constants.EXPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                                .build()
                ),

                systemProperty("providerWithMetadata").value(providerWithMetadata),
                systemProperty("providerWithMetadataInMetaInf").value(providerWithMetadataInMetaInf),
                systemProperty("providerWithoutMetadata").value(providerWithoutMetadata),
                systemProperty("consumerWithMetadata").value(consumerWithMetadata),
                systemProperty("consumerWithoutMetadata").value(consumerWithoutMetadata)
        );

    }

    @Before
    public void before() {
        helper = new OSGiHelper(context);
    }

    @After
    public void after() {
        helper.dispose();
    }

    private static File getTemporaryFile(String name) throws IOException {
        if (!TMP.exists()) {
            TMP.mkdirs();
            TMP.deleteOnExit();
        }
        File file = File.createTempFile(name, ".jar", TMP);
        //File file = new File(TMP, name + ".jar");
        if (file.exists()) {
            file.delete();
        }
        file.deleteOnExit();
        return file;
    }

    @Test
    public void installProviderWithMetadata1() throws BundleException, InvalidSyntaxException, Exception {
        String url = context.getProperty("providerWithMetadata");
        Assert.assertNotNull(url);
        Bundle bundle = context.installBundle("ipojo:" + url);
        bundle.start();

        assertBundle("Provider");

        helper.waitForService(Hello.class.getName(), null, 5000);
        assertValidity();
        Assert.assertNotNull(context.getServiceReference(Hello.class.getName()));
    }


    @Test
    public void installProviderWithMetadata2() throws BundleException, InvalidSyntaxException, IOException {
        String url = context.getProperty("providerWithMetadataInMetaInf");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url).start();
        assertBundle("Provider");
        helper.waitForService(Hello.class.getName(), null, 5000);
        assertValidity();
        Assert.assertNotNull(context.getServiceReference(Hello.class.getName()));
    }

    @Test
    public void installProviderWithoutMetadata() throws BundleException, InvalidSyntaxException, IOException {
        String url = context.getProperty("providerWithoutMetadata");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url).start();
        assertBundle("Provider");
        helper.waitForService(Hello.class.getName(), null, 5000);
        assertValidity();
        Assert.assertNotNull(context.getServiceReference(Hello.class.getName()));
    }

    @Test
    public void installConsumerWithMetadata() throws BundleException, InvalidSyntaxException, IOException {
        String url = context.getProperty("providerWithoutMetadata");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url).start();
        assertBundle("Provider");

        String url2 = context.getProperty("consumerWithMetadata");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url2).start();
        assertBundle("Consumer");
        helper.waitForService(Hello.class.getName(), null, 5000);
        // Wait for activation.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertValidity();
        Assert.assertNotNull(context.getServiceReference(Hello.class.getName()));
    }

    @Test
    public void installConsumerWithoutMetadata() throws BundleException, InvalidSyntaxException, IOException {
        String url = context.getProperty("providerWithMetadataInMetaInf");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url).start();
        assertBundle("Provider");
        helper.waitForService(Hello.class.getName(), null, 5000);

        String url2 = context.getProperty("consumerWithoutMetadata");
        Assert.assertNotNull(url);
        context.installBundle("ipojo:" + url2).start();
        assertBundle("Consumer");
        // Wait for activation.
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertValidity();
        Assert.assertNotNull(context.getServiceReference(Hello.class.getName()));
    }

    /**
     * Gets a regular bundle containing metadata file
     *
     * @return the url of the bundle
     * @throws java.io.IOException
     */
    public static String providerWithMetadata() throws IOException {
        InputStream is = TinyBundles.bundle()
                .add("metadata.xml", OnlineManipulatorTest.class.getClassLoader().getResource("provider.xml"))
                .add(MyProvider.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                .build();

        File out = getTemporaryFile("providerWithMetadata");
        StreamUtils.copyStream(is, new FileOutputStream(out), true);
        return out.toURI().toURL().toExternalForm();
    }

    /**
     * Gets a regular bundle containing metadata file in the META-INF directory
     *
     * @return the url of the bundle
     * @throws java.io.IOException
     */
    public static String providerWithMetadataInMetaInf() throws IOException {
        InputStream is = TinyBundles.bundle()
                .add("META-INF/metadata.xml", OnlineManipulatorTest.class.getClassLoader().getResource("provider.xml"))
                .add(MyProvider.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                .build();

        File out = getTemporaryFile("providerWithMetadataInMetaInf");
        StreamUtils.copyStream(is, new FileOutputStream(out), true);
        return out.toURI().toURL().toExternalForm();
    }

    /**
     * Gets a provider bundle which does not contain the metadata file.
     *
     * @return the url of the bundle + metadata
     * @throws java.io.IOException
     */
    public static String providerWithoutMetadata() throws IOException {
        InputStream is = TinyBundles.bundle()
                //.addResource("metadata.xml", this.getClass().getClassLoader().getResource("provider.xml"))
                .add(MyProvider.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Provider")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                .build();

        File out = getTemporaryFile("providerWithoutMetadata");
        StreamUtils.copyStream(is, new FileOutputStream(out), true);
        String url = out.toURI().toURL().toExternalForm();

        return url + "!" + OnlineManipulatorTest.class.getClassLoader().getResource("provider.xml");
    }

    /**
     * Gets a consumer bundle using annotation containing the instance
     * declaration in the metadata.
     *
     * @return the url of the bundle
     * @throws java.io.IOException
     */
    public static String consumerWithMetadata() throws IOException {
        InputStream is = TinyBundles.bundle()
                .add("metadata.xml", OnlineManipulatorTest.class.getClassLoader().getResource("consumer.xml"))
                .add(Consumer.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Consumer")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                .build();

        File out = getTemporaryFile("consumerWithMetadata");
        StreamUtils.copyStream(is, new FileOutputStream(out), true);
        return out.toURI().toURL().toExternalForm();
    }

    /**
     * Gets a consumer bundle using annotation that does not contain
     * metadata
     *
     * @return the url of the bundle + metadata
     * @throws java.io.IOException
     */
    public static String consumerWithoutMetadata() throws IOException {
        InputStream is = TinyBundles.bundle()
                .add(Consumer.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "Consumer")
                .set(Constants.IMPORT_PACKAGE, "org.apache.felix.ipojo.test.online.services")
                .build();

        File out = getTemporaryFile("consumerWithoutMetadata");
        StreamUtils.copyStream(is, new FileOutputStream(out), true);
        String url = out.toURI().toURL().toExternalForm();

        return url + "!" + OnlineManipulatorTest.class.getClassLoader().getResource("consumer.xml");
    }


    public void dumpServices() throws InvalidSyntaxException {
        ServiceReference[] refs = context.getAllServiceReferences(null, null);
        System.out.println(" === Services === ");
        for (ServiceReference ref : refs) {
            String[] itf = (String[]) ref.getProperty(Constants.OBJECTCLASS);
            System.out.println(itf[0]);
        }
        System.out.println("====");
    }

    public void dumpBundles() throws InvalidSyntaxException {
        Bundle[] bundles = context.getBundles();
        System.out.println(" === Bundles === ");
        for (Bundle bundle : bundles) {
            String sn = bundle.getSymbolicName();
            System.out.println(sn);
        }
        System.out.println("====");
    }

    private void assertBundle(String sn) {
        for (Bundle bundle : context.getBundles()) {
            if (bundle.getSymbolicName().equals(sn)
                    && bundle.getState() == Bundle.ACTIVE) {
                return;
            }

        }
        Assert.fail("Cannot find the bundle " + sn);
    }

    private void assertValidity() {
        try {
            ServiceReference[] refs = context.getServiceReferences(Architecture.class.getName(), null);
            Assert.assertNotNull(refs);
            for (ServiceReference ref : refs) {
                InstanceDescription id = ((Architecture) context.getService(ref)).getInstanceDescription();
                int state = id.getState();
                Assert.assertEquals("State of " + id.getName(), ComponentInstance.VALID, state);
            }
        } catch (InvalidSyntaxException e) {
            Assert.fail(e.getMessage());
        }

    }


}
