/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs.configfiles;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

import hudson.model.FreeStyleBuild;
import hudson.util.Secret;

public class RegistryHelperTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();
    private StandardUsernameCredentials user;
    private StringCredentials token;

    @Before
    public void setUp() throws Exception {
        user = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, "privateId", "dummy desc", "myuser", "mypassword");
        token = new StringCredentialsImpl(CredentialsScope.GLOBAL, "privateToken", "dummy desc", Secret.fromString("mysecret"));
        CredentialsStore store = CredentialsProvider.lookupStores(j.getInstance()).iterator().next();
        store.addCredentials(Domain.global(), user);
        store.addCredentials(Domain.global(), token);
    }

    @Test
    public void test_registry_credentials_resolution() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", user.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", null, "@user1 user2");

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertFalse(resolvedCredentials.isEmpty());
        assertEquals(1, resolvedCredentials.size());

        Assertions.assertThat(resolvedCredentials.keySet().contains(privateRegistry.getUrl()));
        Assertions.assertThat(resolvedCredentials.get(privateRegistry.getUrl())).isEqualTo(user);
    }

    @Test
    public void test_registry_auth_token_credentials_resolution() throws Exception {
        NPMRegistry privateRegistry = new NPMRegistry("https://private.organization.com/", token.getId(), null);
        NPMRegistry officalRegistry = new NPMRegistry("https://registry.npmjs.org/", token.getId(), "@user1 user2");

        FreeStyleBuild build = j.createFreeStyleProject().createExecutable();

        RegistryHelper helper = new RegistryHelper(Arrays.asList(privateRegistry, officalRegistry));
        Map<String, StandardCredentials> resolvedCredentials = helper.resolveCredentials(build);
        assertFalse(resolvedCredentials.isEmpty());
        assertEquals(2, resolvedCredentials.size());

        Assertions.assertThat(resolvedCredentials.keySet().contains(privateRegistry.getUrl()));
        Assertions.assertThat(resolvedCredentials.get(privateRegistry.getUrl())).isEqualTo(token);
        Assertions.assertThat(resolvedCredentials.get(officalRegistry.getUrl())).isEqualTo(token);
    }
}
