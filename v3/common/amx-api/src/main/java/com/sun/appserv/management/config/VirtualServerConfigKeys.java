/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */


package com.sun.appserv.management.config;

/**
	Keys for use with {@link HTTPServiceConfig#createVirtualServerConfig}.
 */
public interface VirtualServerConfigKeys
{
	/** comma-separated list of http-listeners */
	public final static String	HTTP_LISTENERS_KEY		= "http-listeners";
	public final static String	DEFAULT_WEB_MODULE_KEY	= "default-web-module";
	public final static String	LOG_FILE_KEY			= "log-file";
	public final static String	DOC_ROOT_KEY			= "docroot";

	public static final String	DOC_ROOT_PROPERTY_KEY	= PropertiesAccess.PROPERTY_PREFIX + "docroot";
	public static final String	ACCESS_LOG_PROPERTY_KEY = PropertiesAccess.PROPERTY_PREFIX + "accesslog";
	
	
	/**
		Possible values:
		<ul>
		<li>{@link #STATE_DISABLED}</li>
		<li>{@link #STATE_OFF}</li>
		<li>{@link #STATE_ON}</li>
		</ul>
	*/
	public final static String	STATE_KEY				= "State";
	
	/** One possible value for {@link #STATE_KEY} */
	public static final String	STATE_DISABLED	= "disabled";
	/** One possible value for {@link #STATE_KEY} */
	public static final String	STATE_OFF	= "off";
	/** One possible value for {@link #STATE_KEY} */
	public static final String	STATE_ON	= "on";
}
