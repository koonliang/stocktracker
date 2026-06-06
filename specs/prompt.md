# 001-frontend-prototype
This is a frontend and backend project that allows user to track and monitor stocks.
Tech stack to be used:
- Frontend: React Vite
- Backend: Java Quarkus on AWS Lambda

Initial Features:
- Portfolio Dashboard
- Creation of Watchlist
- Import/Export Transaction
- Stock Analysis

For this initial feature, I just need the frontend prototype without backend implementation.
The frontend design must not look like it's vibe-coded. Use the frontend-design skill when creating the prototype.

# 002-connect-frontend-backend
Link the frontend features with the backend. Backend stack will be Java Quarkus with MySQL db.
For local development, include docker compose with frontend/backend app and MySQL db.

# 003-ci-cd-aws
- CI feature: github action for PR creation and merge to main
- CD feature to AWS via terraform: deploy backend to AWS lambda, provision MySQL RDS, frontend to S3 bucket. 
Frontend flow: Cloudflare CDN -> S3
Backend flow: Lambda -> MySQL

# 004-selenium-regression-tests
- for regression testing, i want to use selenium with java for automated web testing
- this should be triggered as part of ci pipeline (run in headless mode)

# 005-user-authentication
- for dev mode, user id and password will be sufficient
- for production mode in aws, suggest if it should integrate with Cognito or use DynamoDB (include a cost analysis)
- include an account sign up feature and reset password