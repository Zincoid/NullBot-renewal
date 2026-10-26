@param String title
@param String subtitle

<svg width="720" height="270" viewBox="0 0 480 180" fill="none" xmlns="http://www.w3.org/2000/svg">
    <defs>
        <linearGradient id="glow" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0" stop-color="#4C8DFF" stop-opacity="0.22"/>
            <stop offset="1" stop-color="#4C8DFF" stop-opacity="0"/>
        </linearGradient>
    </defs>

    <rect width="480" height="180" fill="#0B0D12"/>
    <rect width="480" height="180" fill="url(#glow)"/>
    <rect x="0" y="0" width="6" height="180" fill="#4C8DFF"/>

    <text x="34" y="52" font-family="JetBrains Mono Bold" font-size="13" letter-spacing="2.4"
          fill="#8FA0B8">HELLO PLUGIN</text>
    <text x="32" y="108" font-family="Bernard MT Condensed" font-size="42"
          fill="#FFFFFF">${title}</text>
    <text x="34" y="146" font-family="JetBrains Mono Regular" font-size="16"
          fill="#C5CCDA">${subtitle}</text>
</svg>
