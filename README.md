This library can be used for (amongst other things) testing webservices. For example:

```python
endpoint ?= null
inputFile ?= "input.xml"
expectedFile ?= "expected.xml"
soapAction ?= ""

result = http("POST", endpoint, inputFile, tuple("SOAPAction", soapAction))
# Check the http code
validateEquals("Checking response code", 200, result/code)
# Check that the xml equals the expected one
validateXMLEquals("Checking response", expectedFile, result/content)
```

Both request & response need their correct soap elements of course but these are easily constructed using soapui.

Suppose there was something wrong in the xml, you could get:

```
[INFO] Checking response code: 200
[INFO] Checking response: [B@27b92195
[ERROR] Checking response (xdiff): u:/soap:Envelope/soap:Body[0]/ns3:getMyResponse[0]/ns2:MyItem[0]/ns2:Institution[2]/ns2:Payments[0]/ns2:Information[0]/ns2:Amount[0]
< 251.2
> 251.3
```

This means the expected value has "251.2" but in the actual we have "251.3" for the specified field.