const path = require('path');
const webpack = require("webpack");

module.exports = {
    mode: 'development',
    entry: './src/main/resources/static/js/index.tsx',
    output: {
        path: path.resolve(__dirname, 'src/main/resources/static/js'),
        filename: 'bundle.js',
    },
    devServer: {
        static: './src/main/resources/static/js',
        port: 2020,
        hot: true
    },
    module: {
        rules: [
            {
                test: /\.(js|jsx|ts|tsx)$/,
                exclude: /node_modules/,
                use: {
                    loader: 'babel-loader',
                    options: {
                        presets: ['@babel/preset-react', '@babel/preset-env', "@babel/preset-typescript"]

                    },
                },
            },
            {
                test: /\.css$/i,
                use: ['style-loader', 'css-loader'],
            },
        ],
    },
    resolve: {
        extensions: ['.js', '.jsx', '.ts', '.tsx']
    },
    plugins: [
        new webpack.HotModuleReplacementPlugin(),
    ]
};