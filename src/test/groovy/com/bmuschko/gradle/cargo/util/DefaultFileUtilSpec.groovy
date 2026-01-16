/*
 * Copyright 2011 the original author or authors.
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
package com.bmuschko.gradle.cargo.util

import spock.lang.Specification
import spock.lang.TempDir
import spock.lang.Unroll

/**
 * Filename utilities unit tests.
 */
class DefaultFileUtilSpec extends Specification {
    @TempDir
    File temporaryFolder

    FileUtil fileUtil = new DefaultFileUtil()

    def "throws exception for non-existent file"() {
        when:
        fileUtil.getExtension(new File(temporaryFolder, 'unknownfile'))

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "get file extension for file with name '#filename'"() {
        given:
        def file = new File(temporaryFolder, filename)
        file.createNewFile()

        when:
        String extension = fileUtil.getExtension(file)

        then:
        extension == expectedExtension

        where:
        filename   | expectedExtension
        'test.war' | 'war'
        'test'     | ''
    }

    @Unroll
    def "get file extension for directory with name '#dirname'"() {
        given:
        def dir = new File(temporaryFolder, dirname)
        dir.mkdir()

        when:
        String extension = fileUtil.getExtension(dir)

        then:
        extension == expectedExtension

        where:
        dirname    | expectedExtension
        'test'     | ''
        'test-1.0' | ''
    }
}
