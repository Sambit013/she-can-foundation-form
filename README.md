# She Can Foundation – Tech Internship Form

A simple full-stack web application built for the **She Can Foundation** Full Stack Development Internship Task.

## Tech Stack
- **Backend**: Pure Java (`com.sun.net.httpserver`) — zero external dependencies
- **Frontend**: Vanilla HTML, CSS, JavaScript

## Features
- Contact/application form with Name, Email, Message fields
- Server-side form validation
- "Form Submitted Successfully" confirmation screen
- Submissions logged to `submissions.csv`
- Responsive, mobile-friendly design

## How to Run

```bash
# Compile
javac Server.java

# Run
java Server
```

Open http://localhost:8080 in your browser.

## Project Structure
```
├── Server.java   # Java HTTP server (backend)
└── index.html    # Frontend served by Java
```
