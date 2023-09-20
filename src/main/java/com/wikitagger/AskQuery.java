package com.wikitagger;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
import java.util.Map;

interface AskQuery {
    
    @Data
    class Response {
        @SerializedName("query-continue-offset")
        private int queryContinueOffset;
        private Query query;
    }

    @Data
    class Query {
        @SerializedName("printrequests")
        private Request[] requests;
        private Map<String, Results> results;
        private String serializer;
        private int version;
        @SerializedName("meta")
        private Metadata metadata;
    }

    @Data
    class Request {
        private String label;
        private String key;
        private String redi;
        private String typeid;
        private int mode;
    }

    @Data
    class Metadata {
        private String hash;
        private int count;
        private int offset;
        private String source;
        private String time;
    }

    @Data
    class Printouts {
        @SerializedName("All Item ID")
        private List<Integer> allItemID;
    }

    @Data
    class Results {
        private Printouts printouts;
        private String fulltext;
        private String fullurl;
        private int namespace;
        private String exists;
        private String displaytitle;
    }
}

