FROM jpro-base:jdk16-fx17
WORKDIR /anagrams
COPY ./AnagramsJPro-jpro ./Client
COPY ./AnagramsServer ./Server
COPY ./start.sh .
RUN ln -s /usr/share/fonts/truetype/msttcorefonts/*.ttf /anagrams/Client/fonts/
CMD ["./start.sh"]