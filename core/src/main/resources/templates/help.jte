@param String heading
@param String title
@param String count
@param String tip
@param java.util.List<String> names
@param int rows
@param int height
@param String footerLabel
@param String footer
@param String hint

<svg width="1440" height="${height * 3 / 2}" viewBox="0 0 960 ${height}" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.20"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="960" height="${height}" fill="#0B0D12"/>
    <rect width="960" height="${height}" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="${height}" fill="#4C8DFF"/>

    <text x="40" y="50" font-family="JetBrains Maple Mono" font-weight="bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8"># ${heading}_</text>
    <text x="38" y="103" font-family="JetBrains Maple Mono" font-weight="bold" font-size="34"
          fill="#FFFFFF">${title}</text>
    <text x="40" y="130" font-family="JetBrains Maple Mono" font-size="13"
          fill="#C5CCDA">${count}</text>
    <text x="920" y="130" text-anchor="end" font-family="JetBrains Maple Mono" font-size="12"
          fill="#8FA0B8">${tip}</text>
    <rect x="40" y="146" width="880" height="1" fill="#232A36"/>

    @for(int i = 0; i < names.size(); i++)
        <text x="${40 + (i / rows) * 230}" y="${170 + (i % rows) * 26}"
              font-family="JetBrains Maple Mono" font-size="14" fill="#E7ECF5">${names.get(i)}</text>
    @endfor

    <text x="40" y="${height - 36}" font-family="JetBrains Maple Mono" font-weight="bold"
          font-size="11" letter-spacing="1.2" fill="#8FA0B8">${footerLabel}</text>
    <text x="150" y="${height - 36}" font-family="JetBrains Maple Mono" font-size="13"
          fill="#C5CCDA">${footer}</text>
    <text x="920" y="${height - 36}" text-anchor="end" font-family="JetBrains Maple Mono" font-size="12"
          fill="#8FA0B8">${hint}</text>
</svg>
