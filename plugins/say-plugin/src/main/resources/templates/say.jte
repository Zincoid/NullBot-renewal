@param String avatar
@param java.util.List<java.util.List<com.zincoid.say.SayCmd.Segment>> lines
@param java.util.List<com.zincoid.say.SayCmd.Segment> author

<svg width="1320" height="500" viewBox="0 0 1320 500" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="fade" x1="0" y1="0" x2="0.475" y2="0">
            <stop offset="0" stop-color="#0B0D12" stop-opacity="0"/>
            <stop offset="0.35" stop-color="#0B0D12" stop-opacity="0.25"/>
            <stop offset="0.67" stop-color="#0B0D12" stop-opacity="1"/>
            <stop offset="1" stop-color="#0B0D12" stop-opacity="1"/>
        </linearGradient>
    </defs>

    <rect width="1320" height="500" fill="#0B0D12"/>
    @if(!avatar.isBlank())
        <image x="0" y="0" width="420" height="500" preserveAspectRatio="xMidYMid slice"
               href="${avatar}"/>
    @endif
    <rect x="0" y="0" width="1320" height="500" fill="url(#fade)"/>
    <rect x="0" y="0" width="6" height="500" fill="#4C8DFF" opacity="0.85"/>

    @for(int i = 0; i < lines.size(); i++)
        @for(com.zincoid.say.SayCmd.Segment segment : lines.get(i))
            <text x="${segment.x()}" y="${260 - ((lines.size() - 1) * 46 + 56) / 2 + i * 46}"
                  font-family="${segment.family()}" font-size="34" font-style="${segment.style()}"
                  fill="#E7ECF5">${segment.text()}</text>
        @endfor
    @endfor

    @for(com.zincoid.say.SayCmd.Segment segment : author)
        <text x="${segment.x()}" y="${260 + ((lines.size() - 1) * 46 + 56) / 2}"
              font-family="${segment.family()}" font-size="30" font-style="${segment.style()}"
              font-weight="${segment.weight()}" fill="#8FA0B8"
              stroke="${segment.stroke()}" stroke-width="0.6">${segment.text()}</text>
    @endfor
</svg>
