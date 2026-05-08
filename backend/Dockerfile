FROM golang:1.26-alpine

WORKDIR /app

COPY ./src/go.mod ./src/go.sum ./
RUN go mod download

COPY ./src .
COPY locusKey.json .
COPY .env .

RUN go build -o main .

EXPOSE 8080

CMD ["./main"]