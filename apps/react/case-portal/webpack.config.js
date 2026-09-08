/*eslint-disable no-undef*/
const path = require('path')
const HtmlWebpackPlugin = require('html-webpack-plugin')
const Dotenv = require('dotenv-webpack')
const webpack = require('webpack')

// Get version directly from package.json
const packageJson = require('./package.json')
const appVersion = packageJson.version
const buildTime = new Date().toISOString()

/*eslint-disable no-undef*/
module.exports = {
  entry: './src/index.js',
  resolve: {
    modules: [path.resolve(__dirname, 'src'), 'node_modules'],
    preferRelative: true,
  },
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'build/[name].[contenthash].js', // Add content hash for better cache busting
    publicPath: '/cm/',
    clean: true, // Clean dist folder on each build
  },
  optimization: {
    splitChunks: {
      chunks: 'all',
      minSize: 10000,
      maxSize: 250000,
    },
  },
  module: {
    rules: [
      {
        test: /\.(js|jsx)$/,
        include: path.resolve(__dirname, 'src'),
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              '@babel/preset-env',
              ['@babel/preset-react', { runtime: 'automatic' }],
            ],
          },
        },
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
      {
        test: /\.(png|svg|jpg|gif|svg)$/,
        use: ['file-loader'],
      },
      {
        test: /\.(woff(2)?|ttf|eot)(\?v=\d+\.\d+\.\d+)?$/,
        use: ['file-loader'],
      },
      {
        test: /favicon\.ico$/,
        use: [
          {
            loader: 'file-loader',
            options: {
              name: '[name].[ext]',
            },
          },
        ],
      },
    ],
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './public/index.html',
    }),
    new Dotenv({ systemvars: true }),
    new webpack.DefinePlugin({
      'process.env.REACT_APP_VERSION': JSON.stringify(appVersion),
      'process.env.REACT_APP_BUILD_TIME': JSON.stringify(buildTime),
    }),
  ],
  devServer: {
    static: path.join(__dirname, 'public'),
    port: 3001,
    historyApiFallback: {
      index: '/cm/index.html',
      rewrites: [
        { from: /^\/cm\/.*$/, to: '/cm/index.html' },
      ],
    },
  },
  devtool: 'source-map',
}
