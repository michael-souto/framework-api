package com.detrasoft.framework.api.controllers.jackson;

/*
* To use this functionality you need:
*   1º your controller methods need to contain @JsonView annotation
*   2º If you are working with paginated results, add spring.jackson.mapper.DEFAULT_VIEW_INCLUSION = true setting in your application.yaml file
* */
public class ResponseView {
    public static class find {}
    public static class persist {}
    public static class findAll {}
    public static class findById {}
    public static class post {}
    public static class put {}
    public static class patch {}
    public static class delete {}
}
