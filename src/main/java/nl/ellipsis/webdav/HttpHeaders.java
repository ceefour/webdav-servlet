/*
 * Copyright 2018 Ellipsis BV.
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
/**
 *
 * @author Eric van der Steen
 *
 * @see <a href="https://tools.ietf.org/html/rfc4918">RFC 4918</a>
 *
 */
/*
Reference: https://github.com/ellipsisnl/PfxWebDAV/tree/master/PfxWebDAVAPI
 */
package nl.ellipsis.webdav;

public interface HttpHeaders {

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.1</a>
     */
    public static final String DAV = "DAV";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.3</a>
     */
    public static final String DEPTH = "Depth";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.3</a>
     */
    public static final String DESTINATION = "Destination";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.4</a>
     */
    public static final String IF = "If";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.5</a>
     */
    public static final String LOCK_TOKEN = "Lock-Token";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.6</a>
     */
    public static final String OVERWRITE = "Overwrite";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE <a href="https://tools.ietf.org/html/rfc4918#section-10">RFC 4918
     * Section 10.7</a>
     */
    public static final String TIMEOUT = "Timeout";

    /**
     * See {@link IETF RFC 4918}.
     *
     * @SEE
     * <a href="https://msdn.microsoft.com/en-us/library/cc250217.aspx">Microsoft
     * cc250217</a>
     */
    public static final String MS_AUTHOR_VIA = "MS-Author-Via";
}
