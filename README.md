# Cross Agent Tests

### Data Policy

None of these tests should contain customer data such as SQL strings.
Please be careful when adding new tests from real world failures.

### Tests

| Test Files    | Description   |
| ------------- |-------------|
| [rum_loader_insertion_location](rum_loader_insertion_location) | Describe where the RUM loader (formerly known as header) should be inserted. |
| [rum_footer_insertion_location](rum_footer_insertion_location) | Describe where the RUM footer (aka "client config") should be inserted.  These tests do not apply to agents which insert the footer directly after the loader. |
| [rules.json](rules.json) | Describe how url/metric/txn-name rules should be applied. |
| [rum_client_config.json](rum_client_config.json) | These tests dictate the format and contents of the browser monitoring client configuration.  For more information see: [SPEC](https://newrelic.atlassian.net/wiki/display/eng/JavaScript+Agent+Auto-Instrumentation) |
| [rum_cookie.json](rum_cookie.json)      | These tests indicate the format requirements of a valid RUM cookie. |
| [sql_parsing.json](sql_parsing.json) | These tests show how an SQL string should be parsed for the operation and table name. |
| [url_clean.json](url_clean.json) | These tests show how URLs should be cleaned before putting them into a trace segment's parameter hash (under the key 'uri'). |
| [url_domain_extraction.json](url_domain_extraction.json) | These tests show how the domain of a URL should be extracted (for the purpose of creating external metrics). |
| [postgres_explain_obfuscation](postgres_explain_obfuscation) | These tests show how plain-text explain plan output from PostgreSQL should be obfuscated when SQL obfuscation is enabled. |
| [sql_obfuscation](sql_obfuscation) | Describe how agents should obfuscate SQL queries before transmission to the collector. |
| [attribute_configuration](attribute_configuration.json) | These tests show how agents should respond to the various attribute configuration settings.  For more information see: [Attributes SPEC](https://newrelic.atlassian.net/wiki/display/eng/Agent+Attributes) |
| [cat_map](cat_map.json) | These tests cover the new Dirac attributes that are added for the CAT Map project. See the [CAT Map Spec](https://newrelic.jiveon.com/docs/DOC-1798) for details.|
| [labels](labels.json) | These tests cover the Labels for Language Agents project. See the [Labels for Language Agents Spec](https://newrelic.atlassian.net/wiki/display/eng/Labels+for+Language+Agents+-+draft+spec) for details.|
