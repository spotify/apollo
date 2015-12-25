Website!

Deploy website by doing:

```
mvn site site:deploy
cd website
./lingon.js build
cp -r maven build
./lingon.js git:deploy
```
