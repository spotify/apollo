## A simple example processing data from the Spotify API

### Build
`mvn package`

### Run
`java -jar target/spotify-api-example-service.jar`

### Call
```
$ http :8080/albums/new
HTTP/1.1 200 OK
Content-Length: 1364
Date: Thu, 03 Dec 2015 16:30:18 GMT
Server: Jetty(9.3.4.v20151007)

[{"name":"Hör vad du säger men jag har glömt vad du sa","artist":{"name":"Danny Saucedo"}},{"name":"The Only One (Kleerup Remix)","artist":{"name":"Miriam Bryant"}},{"name":"Broken Arrows (Remixes)","artist":{"name":"Avicii"}},{"name":"Handwritten (Revisited)","artist":{"name":"Shawn Mendes"}},{"name":"Jag går nu","artist":{"name":"Melissa Horn"}},{"name":"Bang My Head (feat. Sia & Fetty Wap)","artist":{"name":"David Guetta"}},{"name":"Handwritten (Revisited)","artist":{"name":"Shawn Mendes"}},{"name":"Everglow","artist":{"name":"Coldplay"}},{"name":"Jag hör vad du säger men glömt vad du sa","artist":{"name":"Danny Saucedo"}},{"name":"Regissören","artist":{"name":"Dani M"}},{"name":"Peace Is The Mission: Extended","artist":{"name":"Major Lazer"}},{"name":"Stay","artist":{"name":"Kygo"}},{"name":"Fine By Me","artist":{"name":"Chris Brown"}},{"name":"Be Together","artist":{"name":"Major Lazer"}},{"name":"Peace Is The Mission (Extended)","artist":{"name":"Major Lazer"}},{"name":"Peace Is The Mission: Extended","artist":{"name":"Major Lazer"}},{"name":"Peace Is The Mission: Extended","artist":{"name":"Major Lazer"}},{"name":"Peace is the Mission: Extended","artist":{"name":"Major Lazer"}},{"name":"Peace Is The Mission : Extended","artist":{"name":"Major Lazer"}},{"name":"Adventure Of A Lifetime (Radio Edit)","artist":{"name":"Coldplay"}}]
```

```
$ http :8080/artists/toptracks/se?q=elvis
HTTP/1.1 200 OK
Content-Length: 1110
Date: Thu, 03 Dec 2015 16:31:29 GMT
Server: Jetty(9.3.4.v20151007)

[{"name":"Can't Help Falling in Love","album":{"name":"Blue Hawaii","artist":{"name":"Elvis Presley"}}},{"name":"Blue Christmas","album":{"name":"Elvis' Christmas Album","artist":{"name":"Elvis Presley"}}},{"name":"Jailhouse Rock","album":{"name":"Elvis' Golden Records","artist":{"name":"Elvis Presley"}}},{"name":"Suspicious Minds","album":{"name":"Back In Memphis","artist":{"name":"Elvis Presley"}}},{"name":"A Little Less Conversation - JXL Radio Edit Remix","album":{"name":"Elvis 75 - Good Rockin' Tonight","artist":{"name":"Elvis Presley"}}},{"name":"Always on My Mind - Remastered","album":{"name":"The Essential Elvis Presley","artist":{"name":"Elvis Presley"}}},{"name":"Here Comes Santa Claus (Right Down Santa Claus Lane)","album":{"name":"Elvis' Christmas Album","artist":{"name":"Elvis Presley"}}},{"name":"In the Ghetto","album":{"name":"From Elvis In Memphis","artist":{"name":"Elvis Presley"}}},{"name":"Hound Dog","album":{"name":"Elvis' Golden Records","artist":{"name":"Elvis Presley"}}},{"name":"Don't Be Cruel","album":{"name":"Elvis' Golden Records","artist":{"name":"Elvis Presley"}}}]
```
