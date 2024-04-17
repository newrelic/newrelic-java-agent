# Apache Log4j Json Template Layout

Adds local decorating support for apps using log4j with `JsonTemplateLayout` such as shown in the xml config snippet below:

```xml
<Appenders>
    <Console name="Console" target="SYSTEM_OUT">
        <!-- THIS DOES NOT WORK! Use JsonTemplateLayout with default JsonLayout provided by log4j2 -->
                     <JsonTemplateLayout eventTemplateUri="classpath:JsonLayout.json"/>
    </Console>
</Appenders>
```