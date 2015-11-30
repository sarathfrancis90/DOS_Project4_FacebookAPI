# **DOS – Project 4**

_Submitted by,_

_Joji Jacob_

_UFID:_

_Sarath Francis_

_UFID: 9474-7916_

# Introduction

The Graph API of the Facebook is the primary way to get data in and out of Facebook's platform. It's a low-level HTTP-based API that you can use to query data, post new stories, manage ads, upload photos and a variety of other tasks that an app might need to do.

In this project we have tried to implement an API which supports a subset of functionalities supported by the Graph API.  This API is also HTTP based and is robust and scalable (tested for supporting up to 200,000 users).

# Running the Project

Follow the following steps to execute the project.

1. 1.Unzip the file and navigate to the folder **dos-project-4** in the terminal
2. 2.Type **sbt  **
3. 3.Type **run**
4. 4.Enter ' **2**' to run the **server.http.FbHttpServer**
5. 5.Open a new Terminal window and repeat the **steps 1** to **3**
6. 6.Enter ' **1**' to run the **client.FbSimulator**

The **FbHttpServer** console window displays the number of requests of each kind handled. The results are refreshed in every 5 seconds.

# Facebook Server

All the information in the system is composed of:

Nodes – "Things" like a user, a photo, a post, an album etc.

Edges – The connections between the "things" like a User's photos, page's posts etc.

Fields – The properties of those "things" such as a person's birthday, email etc. or name of a page.

We have used case classes for implementing Nodes and the case class parameters model the Fields of each Node.

The Nodes in the API are:

| **Node** | **Fields** |
| --- | --- |
| _UserNode_ | id, about, birthday, email, first\_name |
| _PageNode_ | id, about description, name, likes |
| _PostNode_ | id, created\_time, description, from, message, to updated\_time |
| _FriendListNode_ | id, name, owner |
| _PhotoNode_ | id, album, created\_time, from, height, name, width, caption |
| _AlbumNode_ | id, count, cover\_photo, created\_time, description, from, name, albu\_type, updated\_time |



 We used hashmaps are used for handling the Edges(connections) between the Nodes.



# REST/HTTP Server

The REST API is implemented using the spray – can library. The HTTP Server handles all the all the HTTP Requests from client. The list of 'end points' handled by the HTTP Server is given below



| **HTTP Method** | **End Points** | **Function** |
| --- | --- | --- |
| _GET_ | /ping | Testing whether server is up and running |
| _POST_ | /user/create | Creates a new user and returns the userID |
| _POST_ | page/create | Creates a new page and returns the pageID |
| _POST_ | /like\_this\_page | User like a page |
| _POST_ | /page/post | Page posting a message |
| _POST_ | /page/photo | Page posting a photo. Have  the option for tagging multiple users |
| _POST_ | /user/post | User post a message |
| _POST_ | /user/photo | User post a photo. has the option for tagging multiple users |
| _POST_ | /user/album | Creates a new album for the requesting user |
| _POST_ | /user/unlike\_page | User unlike a page |
| _GET_ | /user/timeline | User views his/her timeline |
| _GET_ | /user/ownp\_hotos | Returns all the photos posted by the requesting user |
| _GET_ | /user/tagged\_photos | Returns all the photos in which the requesting user is tagged |
| _GET_ | /user/own\_posts | Returns all the posts posted by the requesting user |
| _GET_ | /user/tagged\_posts | Returns all the posts in which the requesting user is tagged |
| _GET_ | /user/get\_albums | User views all  the albums created by him |
| _GET_ | /user/album\_photos | User viewing all the photos of a particular album |
| _GET_ | /user/liked\_pages | User viewing  list of pages that  he /she had liked |
| _GET_ | /page/liked\_users | Page viewing the list of users who have liked the page |



# HTTP Client

We implemented an Actor based HTTP Client which simulates the the behavior of users and pages in the Facebook.

We simulated the user and page activities as per the Facebook statistics of [http://www.statista.com](http://www.statista.com) which can be found [here](http://www.statista.com/statistics/420714/top-facebook-activities-worldwide/).

The important data from the statistics which we used in our simulation are:

| **Statistics** |
| --- |
| Percent of users who click the like button | 64% |
| Average number of pages liked by a Facebook user | 40 |
| Percent of users who post a status, comments on photos or share some post/photo frequently ( **Active Users** ) | 58% |
| Percent of users who uses Facebook just view the time line and news feed ( **Passive Users** ) | 42% |



The Simulator starts by creating **100,000 users** followed by creating **10,000**** pages. FbServer** handles these requests in lighting speed that these requests will be completed in a matter of few seconds.

Once the users and pages are created, the simulator starts activity of users liking the created pages. The number of number likes per each page and percentage of users liking is decided based on the statistics shown in the table above. The total users created is divided into **Active Users** and **Passive Users** in order to follow the statistics.

Activities performed by the Active users are posting a message, posting a photo and viewing the timeline. In turn the passive users just views the timeline. The Active user activities and the passive user activities are simulated in parallel by separate Actors which models the real scenario.