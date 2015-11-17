## A simple calculator

### Build
`mvn package`

### Run
`java -jar target/calculator-service.jar`

### Call
```
$ http :8080/add t1==28 t2==14
HTTP/1.1 200 OK
Content-Length: 2
Date: Tue, 17 Nov 2015 18:01:32 GMT
Server: Jetty(9.3.4.v20151007)

42
```
