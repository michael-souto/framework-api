package com.detrasoft.framework.api.controllers.jackson;

/*
* To use this functionality you need:
*   1º your controller methods need to contain @JsonView annotation
*   2º If you are working with paginated results, add spring.jackson.mapper.DEFAULT_VIEW_INCLUSION = true setting in your application.yaml file
* */
public class ResponseView {

    public static interface ignore {}
    public static interface findAndPersist {}
    public static interface find extends findAndPersist {}
    public static interface persist extends findAndPersist {}

    public static interface findAll extends find {}
    public static interface findById extends  find {}

    public static interface post extends persist {}
    public static interface put extends persist {}
    public static interface patch extends persist {}

    public static interface delete {}
}
