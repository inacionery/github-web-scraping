## GitHub Web Scraping

## Local

```shell
$ gradlew bootRun
```

## Docker

https://hub.docker.com/r/inacionery/github-web-scraping

```shell
$ docker run inacionery/github-web-scraping
```

## Heroku

https://github-web-scraping.herokuapp.com

## Sample

#### Request:

`https://github-web-scraping.herokuapp.com/files?name=inacionery/github-web-scraping`

#### Response:

###### HTTP Code 200

```json
[
   ...
  {
    "bytes": 15835,
    "extension": "java",
    "lines": 738
  },
   ...
]
```

###### HTTP Code 400

```json
inacionery/github-web-scraping repository does not exist.
```