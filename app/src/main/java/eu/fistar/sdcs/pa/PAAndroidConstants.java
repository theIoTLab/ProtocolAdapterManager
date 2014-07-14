/**
 * Copyright (C) 2014 Consorzio Roma Ricerche
 * All rights reserved
 *
 * This file is part of the Protocol Adapter software, available at
 * https://github.com/theIoTLab/ProtocolAdapter .
 *
 * The Protocol Adapter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://opensource.org/licenses/LGPL-3.0
 *
 * Contact Consorzio Roma Ricerche (protocoladapter@gmail.com)
 */

package eu.fistar.sdcs.pa;

/**
 * This class contains some constants useful for the Protocol Adapter
 *
 * @author Marcello Morena
 * @author Alexandru Serbanati
 */
public class PAAndroidConstants {

    public static final String PACKAGE = "eu.fistar.sdcs.pa";
    public static final String PA_LOGTAG = "PA >>>";
    public static final String DA_LOGTAG = "DA >>>";
    public static final String ERRORTAG = "ERROR >>>";
    public static final String WARNINGTAG = "Warning >>>";

    public static class SDCS {
        public static final String ACTION = "eu.fistar.sdcs";
        public static final String PACKAGE = "eu.fistar.sdcs";
    }
    
    public static class SDCS_MESSAGES {
        static final short MSG_TYPE_DEV_REGISTRATION = 7;
        static final short MSG_TYPE_DEV_PROPERTIES_REGISTRATION = 8;
        static final short MSG_TYPE_DATA_PUSH = 11;
        static final short MSG_TYPE_DEV_DEREGISTRATION = 14;

        static final String MSG_CODE_OK_PREFIX = "20";

        static final String EXTRA_NAME_ISSUER = "Issuer";
        static final String EXTRA_NAME_REQID = "requestId";
        static final String EXTRA_NAME_STATUS = "status";
        static final String EXTRA_NAME_CONTENT = "content";
        static final String EXTRA_NAME_CONTENT_TYPE = "content-type";
        static final String EXTRA_NAME_METHOD = "method";
        static final String EXTRA_NAME_PATH = "path";
        static final String EXTRA_NAME_REPLY_ACTION = "replyAction";

        static final String JSON_NAME_CONTAINER = "container";
        static final String JSON_NAME_ID = "id";
        static final String JSON_NAME_SEARCH_STRING = "searchString";
        static final String JSON_NAME_SEARCH_STRINGS = "searchStrings";
        static final String JSON_NAME_APP_ID = "appId";
        static final String JSON_NAME_APPLICATION = "application";
    }

    public static class DEVICE_ADAPTERS {
        //TODO update with string from flattenComponentName
        public static final String HDP_SERVICE_NAME = "eu.fistar.sdcs.pa.da.bthdp.HDPDeviceAdapter";

    }

    public static String[] AVAILABLE_DAS = { DEVICE_ADAPTERS.HDP_SERVICE_NAME };

    public static String getDANameFromComp(String componentName) {
        if (componentName.startsWith("eu.fistar.sdcs.pa/eu.fistar.sdcs.pa.da.bthdp.HDPDeviceAdapter"))
            return DEVICE_ADAPTERS.HDP_SERVICE_NAME;
        return null;
    }



}
