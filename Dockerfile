FROM node:18.16-alpine

ENV NODE_ENV=production

WORKDIR /app

COPY . .

# install dependencies
RUN NODE_ENV=development yarn

# remove unnecessary files to save up on final image size
RUN yarn cache clean

EXPOSE 8080

# run app
CMD ["yarn", "start"]