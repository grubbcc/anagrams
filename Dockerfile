FROM jpro-base:jdk16-fx17
WORKDIR /anagrams
COPY ./AnagramsServer.jar .
COPY ./AnagramsJPro-jpro/ .
COPY ./start.sh .
CMD ["./start.sh"]