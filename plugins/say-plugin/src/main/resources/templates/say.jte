@param String avatar
@param java.util.List<java.util.List<com.zincoid.nullbot.module.render.svg.layout.SvgText.Segment>> lines
@param String author

<svg width="1320" height="500" viewBox="0 0 1320 500" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="fade" x1="0" y1="0" x2="0.475" y2="0">
            <stop offset="0" stop-color="#0E0F11" stop-opacity="0"/>
            <stop offset="0.35" stop-color="#0E0F11" stop-opacity="0.25"/>
            <stop offset="0.67" stop-color="#0E0F11" stop-opacity="1"/>
            <stop offset="1" stop-color="#0E0F11" stop-opacity="1"/>
        </linearGradient>
    </defs>

    <rect width="1320" height="500" fill="#0E0F11"/>
    @if(!avatar.isBlank())
        <image x="0" y="0" width="420" height="500" preserveAspectRatio="xMidYMid slice"
               href="${avatar}"/>
    @endif
    <rect x="0" y="0" width="1320" height="500" fill="url(#fade)"/>
    
    @for(int i = 0; i < lines.size(); i++)
        @if(!lines.get(i).isEmpty())
            <text x="500" y="${260 - ((lines.size() - 1) * 46 + 56) / 2 + i * 46}"
                  font-family="${lines.get(i).get(0).family()}"
                  font-size="34" font-style="${lines.get(i).get(0).emoji() ? "normal" : "italic"}"
                  fill="#F2F3F5">${lines.get(i).get(0).text()}</text>
            @for(int j = 1; j < lines.get(i).size(); j++)
                <text y="${260 - ((lines.size() - 1) * 46 + 56) / 2 + i * 46}"
                      font-family="${lines.get(i).get(j).family()}"
                      font-size="34" font-style="${lines.get(i).get(j).emoji() ? "normal" : "italic"}"
                      fill="#F2F3F5">${lines.get(i).get(j).text()}</text>
            @endfor
        @endif
    @endfor

    <text x="1240" y="${260 + ((lines.size() - 1) * 46 + 56) / 2}" text-anchor="end"
          font-family="Noto Serif CJK SC" font-size="30" font-style="italic" font-weight="bold"
          fill="#CDD3DC">⸺ ${author}</text>
</svg>
