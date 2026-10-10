@echo off
echo Creating 10 books...

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\",\"isbn\":\"978-0132350884\",\"description\":\"A Handbook of Agile Software Craftsmanship\",\"price\":45.99,\"publicationYear\":2008,\"availableQuantity\":100,\"publisher\":\"Prentice Hall\",\"category\":\"Software Engineering\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"The Pragmatic Programmer\",\"author\":\"Andrew Hunt\",\"isbn\":\"978-0201616224\",\"description\":\"From Journeyman to Master\",\"price\":49.99,\"publicationYear\":1999,\"availableQuantity\":80,\"publisher\":\"Addison-Wesley\",\"category\":\"Software Engineering\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Design Patterns\",\"author\":\"Gang of Four\",\"isbn\":\"978-0201633610\",\"description\":\"Elements of Reusable Object-Oriented Software\",\"price\":54.99,\"publicationYear\":1994,\"availableQuantity\":60,\"publisher\":\"Addison-Wesley\",\"category\":\"Software Architecture\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Refactoring\",\"author\":\"Martin Fowler\",\"isbn\":\"978-0134757599\",\"description\":\"Improving the Design of Existing Code\",\"price\":52.99,\"publicationYear\":2018,\"availableQuantity\":75,\"publisher\":\"Addison-Wesley\",\"category\":\"Software Engineering\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Spring in Action\",\"author\":\"Craig Walls\",\"isbn\":\"978-1617294945\",\"description\":\"Covers Spring 5\",\"price\":59.99,\"publicationYear\":2018,\"availableQuantity\":90,\"publisher\":\"Manning\",\"category\":\"Java\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Effective Java\",\"author\":\"Joshua Bloch\",\"isbn\":\"978-0134685991\",\"description\":\"Best Practices for the Java Platform\",\"price\":55.99,\"publicationYear\":2018,\"availableQuantity\":85,\"publisher\":\"Addison-Wesley\",\"category\":\"Java\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Clean Architecture\",\"author\":\"Robert C. Martin\",\"isbn\":\"978-0134494166\",\"description\":\"A Craftsman Guide to Software Structure\",\"price\":47.99,\"publicationYear\":2017,\"availableQuantity\":70,\"publisher\":\"Prentice Hall\",\"category\":\"Software Architecture\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Domain-Driven Design\",\"author\":\"Eric Evans\",\"isbn\":\"978-0321125217\",\"description\":\"Tackling Complexity in the Heart of Software\",\"price\":62.99,\"publicationYear\":2003,\"availableQuantity\":50,\"publisher\":\"Addison-Wesley\",\"category\":\"Software Architecture\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Microservices Patterns\",\"author\":\"Chris Richardson\",\"isbn\":\"978-1617294549\",\"description\":\"With examples in Java\",\"price\":58.99,\"publicationYear\":2018,\"availableQuantity\":65,\"publisher\":\"Manning\",\"category\":\"Microservices\"}"
echo.

curl -X POST http://localhost:8080/api/books -H "Content-Type: application/json" -d "{\"title\":\"Kubernetes in Action\",\"author\":\"Marko Luksa\",\"isbn\":\"978-1617293726\",\"description\":\"Running applications in Kubernetes\",\"price\":64.99,\"publicationYear\":2017,\"availableQuantity\":55,\"publisher\":\"Manning\",\"category\":\"DevOps\"}"
echo.

echo All 10 books created!
