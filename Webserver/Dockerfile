FROM grubbcc/jpro-base:jdk18-fx18
WORKDIR /anagrams
COPY ./AnagramsJPro-jpro ./Client
COPY ./AnagramsServer ./Server
COPY ./start.sh .
RUN ln -s /usr/share/fonts/truetype/msttcorefonts/*.ttf /anagrams/Client/fonts/
CMD ["./start.sh"]
