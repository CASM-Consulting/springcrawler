package com.casm.acled.camunda.variables;

public class Process {

    public static final String LOCK_FIRST = "lockFirst";
    public static final String LOCK_SECOND = "lockSecond";
    public static final String LOCK_THIRD = "lockThird";

    public static final String BUSINESS_KEY = "business_key";

    public static final String FILTER_KEY = "filter_key";
    public static final String PRIORITY = "priority";
    public static final String SOURCE_LIST_NAME = "source_list_name";
    public static final String SOURCE_LIST_ID = "source_list_id";

    public static final String DELETE_REASON_SKIPPED = "delete_reason_skipped";

    //context condition
    public static final String PROCESS = "process";
    public static final String CONTEXT_CONDITION = "context_condition";
    public static final String CONTEXT = "context";

    //processes
    public static final String ENTITY_REVIEW = "entity_review";
    public static final String ADD_ENTITY = "add_entity";
    public static final String FROM = "from";
    public static final String TO = "to";
    public static final String BACK_CODING_CONFIG = "back_coding_config";
    public static final String LIVE_CODING_CONFIG = "live_coding_config";
    public static final String ADD_SOURCE = "add_source";
    public static final String ADD_LOCATION = "add_source";
    public static final String ADD_ACTOR = "add_source";
    public static final String OVERVIEW = "overview";
    public static final String SOURCE_CODE = "source_code";
    public static final String RM_REVIEW = "rm_review";
    public static final String GRM_REVIEW = "grm_review";
    public static final String ALL = "*";

    //actions
    public static final String EDIT = "edit";
    public static final String READ_ONLY = "read_only";
    public static final String HIDE = "hide";
    public static final String SHOW = "show";

    //tasks
    public static final String SOURCE_CODE_TASK = "source_code_task";

    //display vars
    public static final String STICKY = "sticky";
    public static final String TASK_NAME = "task_name";
    public static final String TASK_TYPE = "task_type";

    public static final String TASK_TYPE_ADMIN = "admin";
    public static final String TASK_TYPE_GENERAL = "general";
    public static final String TASK_TYPE_FEEDBACK = "feedback";
    public static final String TASK_TYPE_SOURCE_CODE = "source_code";
    public static final String TASK_TYPE_REVIEW_ACTOR = "review_actor";
    public static final String TASK_TYPE_REVIEW_LOCATION = "review_location";
    public static final String TASK_TYPE_REVIEW_SOURCE = "review_source";


    // Special vars used in off-system coding forms.
    public static final String DOWNLOAD_PATH = "download_path";
    public static final String PROPOSED_UPLOAD_CHANGES = "proposed_upload_changes";
    public static final String OFF_SYSTEM_CODING_SHEET = "off_system_coding_sheet";

    public static final String CANDIDATE_GROUP = "candidate_group";
}
