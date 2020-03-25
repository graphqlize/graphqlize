## GraphQLize

[GraphQLize](https://www.graphqlize.org), an open-source Clojure (JVM) library for developing GraphQL API instantly from your existing PostgreSQL and MySQL databases.

It aims to simplify the effort required to expose GraphQL APIs over relational databases.

[![Clojars Project](https://img.shields.io/clojars/v/org.graphqlize/graphqlize.svg)](https://clojars.org/org.graphqlize/graphqlize)

<a href="https://discord.gg/akkdPqf"><img src="https://img.shields.io/badge/chat-discord-brightgreen.svg?logo=discord&style=flat"></a>
<a href="https://twitter.com/intent/follow?screen_name=GraphQLize"><img src="https://img.shields.io/badge/Follow-GraphQLize-blue.svg?style=flat&logo=twitter"></a>
<a href="https://tinyletter.com/graphqlize-org"><img src="https://img.shields.io/badge/newsletter-subscribe-yellow.svg?style=flat"></a>

> GraphQLize is at its early stages now. The objective of this early release is to get early feedback from the community. **It is not production-ready yet!**


## Rationale

In the JVM ecosystem, developing GraphQL APIs to expose the data from the relational databases requires a lot of manual work. Right from defining the GraphQL schemas (either code-first or schema-first) to wiring them with resolvers and the database access logic, we spend a significant amount of our development time.

In addition to this, we also need to take care of optimizing the underlying SQL queries to avoid problems like N+1 queries. We have to account the maintenance of the resulting codebase as well!

GraphQLize will help you to overcome all these shortcomings. It provides you with an efficient GraphQL implementation in just few lines of code.

## What is GraphQLize?

GraphQLize is a JVM library written in Clojure with Java interoperability. The crux of GraphQLize is generating the GraphQL schema and resolving the queries by making use of [JDBC metadata](https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html) provided by the JDBC drivers.

It currently supports Postgres (9.4 & above) and MySQL (8.0 & above).

## Getting Started

Getting started with GraphQLize is simple and involves only a few steps.

1. Add the GraphQLize dependency in your project.
2. Initialize GraphQLize Resolver by providing the Java SQL [data source](https://docs.oracle.com/javase/7/docs/api/javax/sql/DataSource.html).
3. Add a GraphQL API endpoint and use the initialized GraphQlize Resolver in the previous step.

The actual implementation of these steps will vary based on which language (Java, Clojure) and framework (Spring Boot, Spark Java, Pedestal, etc.). Please refer the below links for more details.

| Langauge | Framework(s) |
|----------|------------|
| Java       | [Spring Boot](http://graphqlize.org/docs/getting_started/java/springboot), [Spark Java](http://graphqlize.org/docs/getting_started/java/sparkjava), [Vanilla Java](https://www.graphqlize.org/docs/getting_started/java/vanilla) |
| Kotlin     | [Spring Boot](https://www.graphqlize.org/docs/getting_started/kotlin/springboot), [Ktor](https://www.graphqlize.org/docs/getting_started/kotlin/ktor) |
| Clojure    | [Pedestal](https://www.graphqlize.org/docs/getting_started/clojure/pedestal), [Ring](https://www.graphqlize.org/docs/getting_started/clojure/ring), [Vanilla Clojure](https://www.graphqlize.org/docs/getting_started/clojure/vanilla) |
| Scala      | [Scalatra](https://www.graphqlize.org/docs/getting_started/scala/scalatra)  |

## Is It Production Ready?

It will be in a few months.

The objective of this alpha release is to get early feedback from the community.

There are close to [forty issues](https://github.com/graphqlize/graphqlize/issues?q=is%3Aissue+is%3Aopen+sort%3Acreated-asc) that I am planning to work on in the upcoming months to make it production ready.

You can keep track of the progress by

- Following the [GitHub project board](https://github.com/orgs/graphqlize/projects/1)
- Subscribing to [GraphQLize's newsletter](https://tinyletter.com/graphqlize-org).
- Joining [GraphQLize's Discord](https://discord.gg/akkdPqf).

## How can I contribute?

GraphQLize is at its early stage now, and the codebase and the APIs are not stable yet.

So, at this instant, the best way to contribute is to initiate a chat in [GraphQLize's Discord](https://discord.gg/akkdPqf) channel or raise [a GitHub issue](https://github.com/graphqlize/graphqlize/issues/new) with all the relevant details, and we'll take it from there.

## Oracle & SQL Server Support

One of the design goal of GraphQLize from day one is to support Postgres, MySQL, Oracle & MS SQL Server. To start with, I am focussing on getting it to a production ready state for Postgres & MySQL. After accomplishing this, I will be focusing on the other two.

## Acknowledgements

[PostgREST](http://postgrest.org), [PostGraphile](https://www.graphile.org/postgraphile/), [KeyStoneJs](https://www.keystonejs.com/) and [Hasura](https://hasura.io/) are the inspiration behind GraphQLize.

GraphQLize is not possible without the following excellent Clojure libraries.

- [HoneySQL](https://github.com/jkk/honeysql)
- [Lacinia](https://github.com/walmartlabs/lacinia)
- [next-jdbc](https://github.com/seancorfield/next-jdbc)
- [inflections](https://github.com/r0man/inflections-clj)
- [data-json](https://github.com/clojure/data.json)

The samples in the documentation of GraphQLize uses the [Sakila](https://www.jooq.org/sakila) database from [JOOQ](https://www.jooq.org) extensively.

## License

The use and distribution terms for this software are covered by the [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0). By using this software in any fashion, you are agreeing to be bound by the terms of this license.
