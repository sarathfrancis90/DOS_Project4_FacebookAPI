# **DOS – Project 4**

_Joji Jacob_

_Sarath Francis_

# Running the Project

Follow these steps to execute the project

1. Unzip the file and navigate to the folder **dos-project-4** in the terminal
2. Type **sbt**
3. Type **run**
4. Run the **server.http.FbHttpServer**
5. Open a new Terminal window and repeat the **steps 1** to **3**
6. Run the **client.FbSimulator**

The **FbHttpServer** console window displays the number of requests of each kind handled. The results are refreshed every 5 seconds.

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

We implemented an Actor based HTTP Client which simulates the the behavior of users and pages in Facebook.