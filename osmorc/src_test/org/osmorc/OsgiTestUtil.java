/*
 * Copyright (c) 2007-2009, Osmorc Development Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright notice, this list
 *       of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this
 *       list of conditions and the following disclaimer in the documentation and/or other
 *       materials provided with the distribution.
 *     * Neither the name of 'Osmorc Development Team' nor the names of its contributors may be
 *       used to endorse or promote products derived from this software without specific
 *       prior written permission.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.osmorc;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.io.Decompressor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:robert@beeger.net">Robert F. Beeger</a>
 */
public final class OsgiTestUtil {
  public static void extractProject(String projectName, Path projectDir) throws IOException {
    var projectZip = getTestDataDir().resolve(projectName + ".zip");
    assertTrue(projectZip.toString(), Files.isRegularFile(projectZip));
    new Decompressor.Zip(projectZip).extract(projectDir);
  }

  private static Path ourTestDataDir;

  private static Path getTestDataDir() {
    if (ourTestDataDir == null) {
      ourTestDataDir = Path.of(OsgiTestUtil.class.getResource("/").getFile(), "../../../testdata");
      if (!Files.exists(ourTestDataDir)) {
        ourTestDataDir = Path.of(OsgiTestUtil.class.getResource("").getFile(), "../../../../../testdata");
      }
      if (!Files.exists(ourTestDataDir)) {
        ourTestDataDir = Path.of(PathManager.getHomePath(), "contrib/osmorc/testdata");
      }
      assertTrue(ourTestDataDir.toString(), Files.isDirectory(ourTestDataDir));
    }

    return ourTestDataDir;
  }
}
