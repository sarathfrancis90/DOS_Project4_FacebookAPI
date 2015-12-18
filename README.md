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

The REST API is implemented using the spray–can library. The list of end points handled by the HTTP Server is given below



| **HTTP Method** | **End Points** | **Function** |
| --- | --- | --- |
| _GET_ | /ping | To test if server is running |
| _POST_ | /user/create | Creates a new user and returns the userID |
| _POST_ | page/create | Creates a new page and returns the pageID |
| _POST_ | /like\_this\_page | For a user to like a page |
| _POST_ | /page/post | Creates a post by a page |
| _POST_ | /page/photo | Creates a photo posted by a page |
| _POST_ | /user/post | Creates a post by a user; users can tag other users in posts |
| _POST_ | /user/photo | Creates a photo by a user; users can tag other users in caption |
| _POST_ | /user/album | Creates a new album for the requesting user |
| _POST_ | /user/unlike\_page | For a user to un-like a page |
| _GET_ | /user/timeline | View timeline for users |
| _GET_ | /user/own\_photos | View photos posted by user |
| _GET_ | /user/tagged\_photos | View photos the user is tagged in |
| _GET_ | /user/own\_posts | View posts made by user |
| _GET_ | /user/tagged\_posts | View posts the user is tagged in |
| _GET_ | /user/get\_albums | View albums the user owns  |
| _GET_ | /user/album\_photos | View photos in an album owned by user |
| _GET_ | /user/liked\_pages | View pages liked by user |
| _GET_ | /page/liked\_users | View users that have liked a page |


# HTTP Client

We implemented an Actor based HTTP Client which simulates the the behavior of users and pages in Facebook.