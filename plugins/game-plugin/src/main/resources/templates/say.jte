@param String avatar
@param java.util.List<String> lines
@param String author

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
        <text x="500" y="${260 - ((lines.size() - 1) * 46 + 56) / 2 + i * 46}"
              font-family="Noto Serif CJK SC" font-size="34" font-style="italic"
              fill="#E7ECF5">${lines.get(i)}</text>
    @endfor

    <text x="1240" y="${260 + ((lines.size() - 1) * 46 + 56) / 2}" text-anchor="end"
          font-family="Noto Serif CJK SC" font-size="30" font-style="italic" font-weight="bold"
          fill="#8FA0B8" stroke="#8FA0B8" stroke-width="0.6">⸺ ${author}</text>
</svg>
