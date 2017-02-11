var webpack = require('webpack');
var HtmlWebpackPlugin = require('html-webpack-plugin');
var ExtractTextPlugin = require('extract-text-webpack-plugin');
var helpers = require('./helpers');

module.exports = {
    entry: {
        'polyfills': './src/app-angular/polyfills.ts',
        'vendor': './src/app-angular/vendor.ts',
        'main-search': './src/app/main.search.ts',
        'main-grammar': './src/app/main.grammar.ts'
    },

    resolve: {
        extensions: ['', '.js', '.ts']
    },

    module: {
        loaders: [
            {
                test: /\.ts$/,
                loaders: ['awesome-typescript-loader', 'angular2-template-loader']
            },
            {
                test: /\.html$/,
                loader: 'html'
            },
            {
                test: /\.(png|jpe?g|gif|svg|woff|woff2|ttf|eot|ico)$/,
                loader: "file-loader"
//                loader: 'file?name=assets/[name].[hash].[ext]'
            },
            {
                test: /\.css$/,
                exclude: helpers.root('src', 'app'),
                loader: ExtractTextPlugin.extract('style', 'css?sourceMap')
            },
            {
                test: /\.css$/,
                include: helpers.root('src', 'app'),
                loader: 'raw'
            }
        ]
    },

    plugins: [
        new webpack.optimize.CommonsChunkPlugin({
            name: ['vendor', 'polyfills']
        }),
        new HtmlWebpackPlugin({
        	inject: false,
            filename: 'index.html',
            template: 'src/index.html'
        }),
        new HtmlWebpackPlugin({
        	inject: false,
            filename: 'download.html',
            template: 'src/download.html'
        }),
        new HtmlWebpackPlugin({
        	inject: true,
        	showErrors: true,
        	chunks: ['main-search', 'vendor', 'polyfills'],
            filename: 'korpus.html',
            template: 'src/korpus.html'
        }),
        new HtmlWebpackPlugin({
            inject: true,
            chunks: ['main-grammar', 'vendor', 'polyfills'],
            filename: 'grammar.html',
            template: 'src/grammar.html'
        })
    ]
};
