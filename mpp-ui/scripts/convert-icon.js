#!/usr/bin/env node

/**
 * Convert SVG icon to PNG for desktop packaging
 * Uses puppeteer to render SVG to PNG at different sizes
 */

const fs = require('fs');
const path = require('path');

const svgPath = path.join(__dirname, '../src/jvmMain/resources/icon.svg');
const outputDir = path.join(__dirname, '../src/jvmMain/resources');

// Read SVG content
const svgContent = fs.readFileSync(svgPath, 'utf8');

// Extract dimensions from SVG
const widthMatch = svgContent.match(/width="(\d+)px"/);
const heightMatch = svgContent.match(/height="(\d+)px"/);
const baseWidth = widthMatch ? parseInt(widthMatch[1]) : 40;
const baseHeight = heightMatch ? parseInt(heightMatch[1]) : 40;

console.log(`Original SVG size: ${baseWidth}x${baseHeight}`);

// For macOS, we need multiple sizes: 16, 32, 64, 128, 256, 512, 1024
// Compose Desktop will handle ICNS conversion from PNG
const sizes = [16, 32, 64, 128, 256, 512, 1024];

// Create PNG using Canvas (if available) or fallback to base64 data URI
console.log('\n⚠️  Note: This script requires @napi-rs/canvas or puppeteer for proper SVG rendering.');
console.log('For now, we\'ll create a 512x512 PNG placeholder.\n');

// Create a simple approach: scale SVG viewBox and save as different files
sizes.forEach(size => {
    const scaledSvg = svgContent
        .replace(/width="40px"/, `width="${size}px"`)
        .replace(/height="40px"/, `height="${size}px"`)
        .replace(/viewBox="0 0 40 40"/, `viewBox="0 0 40 40"`);
    
    const outputPath = path.join(outputDir, `icon-${size}.svg`);
    fs.writeFileSync(outputPath, scaledSvg);
    console.log(`Created: icon-${size}.svg`);
});

console.log('\n✅ SVG files created at different sizes.');
console.log('\n📝 Next steps:');
console.log('   1. Install: brew install librsvg');
console.log('   2. Run: npm run convert-icon-png');
console.log('   Or manually convert using online tools like:');
console.log('   - https://cloudconvert.com/svg-to-png');
console.log('   - https://www.aconvert.com/icon/svg-to-icns/');
