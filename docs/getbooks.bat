@echo off
echo getting 10 books...

curl -X GET http://localhost:8080/api/books/1 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/2 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/3 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/4 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/5 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/6 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/7 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/8 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/9 -H "Content-Type: application/json" 
echo.
curl -X GET http://localhost:8080/api/books/10 -H "Content-Type: application/json" 
echo.
echo All 10 books retrieved!
