# jsp-3 Instrumentation Module

## Injection of the Real User Monitoring Script Via JSP Tag Libraries
Prior to the additions to this instrumentation module, the only way to inject the RUM script into JSPs was during the 
compilation phase of the Jasper compiler, which injects the script into the HTML `<head>` element, if present in the page
source.

Some applications use custom JSP tag libraries to create the head tag (and other HTML page elements). In this scenario, the
RUM script will not be injected because of the way the Jasper compiler instrumentation detects the head tag. This
instrumentation module weaves the `SimpleTagSupport` and `TagSupport` classes to detect the creation of head elements via the
tag execution and inject the RUM script at that time.

### Configuration
To enable tag library instrumentation, both of the following settings must be `true`:
```
  browser_monitoring:
    auto_instrument: true
    tag_lib_instrument: true
```

By default, the `tag_lib_instrument` setting is `false`.

The instrumentation will use the regular expression pattern of `<head>` to detect the start of HTML head elements.
If a tag library emits a more complex head start element, the regular expression can be modified via the `tag_lib_head_pattern`
config setting. For example:
```
  browser_monitoring:
    tag_lib_head_pattern: '<head.*>'
```
The regular expression will be compiled to be case-insensitive. If the defined regular expression is invalid it will default to `<head>`.


### Requirements for Script Injection
The following are the requirements for the RUM script to be injected from instrumented tag libraries:
- The application must utilize version 3 of the JSP libraries (jakarta namespace)
- Only the first instance of a `<head>` string emitted by a tag library will be considered, regardless of context (in a comment, for example)
- The custom tag must extend either the [SimpleTagSupport](https://jakarta.ee/specifications/pages/3.0/apidocs/jakarta/servlet/jsp/tagext/simpletagsupport) or 
[TagSupport](https://jakarta.ee/specifications/pages/3.0/apidocs/jakarta/servlet/jsp/tagext/tagsupport) classes
- The `<head>` element must be emitted from the tag class via the `print(String s)` or `println(String s)` methods of the JspWriter
fetched from a [JspContext.getOut()](https://jakarta.ee/specifications/pages/3.0/apidocs/jakarta/servlet/jsp/jspcontext) call
- The head start tag (`<head>`) must be totally emitted in a single `print`/`println` call. For example, these are all valid:
```java
    out.println("<head>");
    out.print("<head>");
    out.println("<head><title>Test Site</title></head>");
    out.println("<head><title>");
    out.println("<he" + "ad>")
```

### Example SimpleTagSupport Use Case
##### Tag Library Class
```java
    // Package/imports omitted
    // Also assumes the existence of a valid, corresponding tld file
    public class SimpleTagExample extends SimpleTagSupport {
        public void doTag() throws JspException, IOException {
            JspWriter out = getJspContext().getOut();
            out.println("<head>\n" +
                    "<meta charset=\"ISO-8859-1\">\n" +
                    "<title>Test Site</title>" +
                    "</head>");
        }
    }
```

##### JSP
```html
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="simple" uri="http://com.sun.simpletagexample"%>
<!DOCTYPE html>
<html>
<simple:SimpleTag/>

<body>
	<h1>Sample JSP</h1>
</body>
</html>
```

In this scenario, the `SimpleTagSupport` instrumentation will detect the output of the head tag and inject the RUM script
just like the Jasper compiler instrumentation, which will result in the following HTML:
```html

<!DOCTYPE html>
<html>
<head>
<script type="text/javascript"><!-- RUM script here --></script>
<meta charset="ISO-8859-1">
<title>Test Site</title></head>

<body>
    <h1>Sample JSP</h1>
</body>
</html>
```

### Example TagSupport Use Case
Tag libraries that extend the `TagSupport` class work in largely the same way as the `SimpleTagSupport` based tag libraries. The instrumentation 
will detect head element generation via the `doStartTag` or `doEndTag` method calls and will inject the RUM script appropriately.
